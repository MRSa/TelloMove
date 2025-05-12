package jp.osdn.gokigen.tellomove.file

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.experimental.and

object NalToMp4Converter
{
    // ----- Gemini 2.5 Flashで生成
    private const val TAG = "NalToMp4Converter"
    private const val VIDEO_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC // H.264 (AVC)ビデオ用
    private const val FRAME_RATE = 30 // 想定されるフレームレート (fps)
    private const val I_FRAME_INTERVAL_SECONDS = 1 // 想定されるIフレーム間隔 (秒)
    private const val DEFAULT_WIDTH = 960 // SPSから取得できない場合のデフォルト幅
    private const val DEFAULT_HEIGHT = 720 // SPSから取得できない場合のデフォルト高さ

    /**
     * 生のH.264 NALユニットファイルをMP4ファイルに変換し、MediaStoreに保存します。
     *
     * @param context アプリケーションのコンテキスト。
     * @param nalInputFile 入力NALユニットファイルを表すFileオブジェクト。
     * @param outputFileName 出力MP4ファイルに希望する名前 (例: "my_video.mp4")。
     * @param listener 進行状況の更新や完了のためのオプションのリスナー。
     */
    suspend fun convertNalToMp4(
        context: Context,
        nalFileName: String,
        outputFileName: String,
        outputDir :String = "TelloMove",
        listener: ConversionListener? = null
    ) = withContext(Dispatchers.IO) {
        var muxer: MediaMuxer? = null
        var nalExtractor: NalUnitExtractor? = null
        //var outputStream: OutputStream? = null // このOutputStreamはMediaStoreに保存されたファイルのハンドルであり、MediaMuxerはFileDescriptorを使用します
        var contentUri: Uri? = null
        var videoTrackIndex = -1
        var presentationTimeUs: Long = 0 // プレゼンテーションタイムスタンプ (マイクロ秒)

        try
        {
            // -----------
            val baseDir = context.getExternalFilesDir(null)
            val filePath = "${baseDir?.absolutePath}/$outputDir/$nalFileName"
            val nalInputFile = File(filePath)

            // 1. 出力MP4ファイルのMediaStoreエントリを準備する
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.TITLE, outputFileName)
                put(MediaStore.MediaColumns.DISPLAY_NAME, outputFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                {
                    // Android Q (API 29) 以降では、相対パスを指定できる
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$outputDir/")
                    put(MediaStore.Video.Media.IS_PENDING, true)
                }
            }

            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            contentUri = resolver.insert(collection, contentValues)
            if (contentUri == null)
            {
                // ----
                Log.v(TAG, "ERROR> CANNOT ENTRY in the MediaStore. $outputFileName")
                throw IOException("cannot entry in the MediaStore : $outputFileName")
            }
            // ParcelFileDescriptorを取得し、MediaMuxerに渡す
            // MediaMuxerは通常、FileDescriptorまたはファイルパスを好む
            val pfd = resolver.openFileDescriptor(contentUri, "w")
            if (pfd == null)
            {
                // ----
                Log.v(TAG, "ERROR> CANNOT GET a ParcelFileDescriptor : $outputFileName")
                throw IOException("cannot get  a ParcelFileDescriptor : $outputFileName")
            }

            // 2. MediaMuxerを初期化する
            muxer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //MediaMuxer(pfd.fileDescriptor, MediaMuxer.OutputFormat.MPEG_4)
                MediaMuxer(pfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            } else {
                // API 26未満では、MediaMuxer.OutputFormatはint型
                // MediaMuxer(pfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG4)
                MediaMuxer("${Environment.DIRECTORY_MOVIES}/$outputDir/$outputFileName", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            }
            pfd.close() // MediaMuxerに渡した後、ParcelFileDescriptorをクローズする

            // 3. NalUnitExtractorを初期化する
            nalExtractor = NalUnitExtractor(FileInputStream(nalInputFile))

            // 4. SPSとPPS NALユニットを見つけてMediaFormatを設定する
            var spsBuffer: ByteBuffer? = null
            var ppsBuffer: ByteBuffer? = null
            // var vpsBuffer: ByteBuffer? = null // HEVC (H.265)の場合
            val csd = mutableListOf<ByteBuffer>() // コーデック固有のデータ (SPS, PPS, VPSなど)

            var nalUnit: ByteBuffer?
            val initialNalUnits = mutableListOf<ByteBuffer>()
            var foundSpsPps = false
            val maxInitialNalUnitsToScan = 100 // SPS/PPSを見つけるために最初のN NALユニットをスキャンする

            // 最初のN NALユニットを読み込み、SPS/PPSを見つける
            while (initialNalUnits.size < maxInitialNalUnitsToScan && !foundSpsPps) {
                nalUnit = nalExtractor.readNextNalUnit()
                if (nalUnit == null) break

                // NALユニットのタイプを抽出 (H.264の場合)
                // NALユニットの最初のバイト (start codeの直後) の下位5ビット
                val nalTypeByteIndex = startCodeLength(nalUnit) ?: 0
                val nalType = (nalUnit.get(nalTypeByteIndex) and 0x1F).toInt()
                Log.d(TAG, "Initial NAL unit type: $nalType, size: ${nalUnit.remaining()}")

                when (nalType) {
                    7 -> { // SPS (Sequence Parameter Set)
                        spsBuffer = nalUnit.asReadOnlyBuffer() // 読み取り専用のコピーを保存
                        csd.add(spsBuffer) // csdに追加
                        Log.d(TAG, "SPSが見つかりました")
                    }
                    8 -> { // PPS (Picture Parameter Set)
                        ppsBuffer = nalUnit.asReadOnlyBuffer() // 読み取り専用のコピーを保存
                        csd.add(ppsBuffer) // csdに追加
                        Log.d(TAG, "PPSが見つかりました")
                    }
                    // HEVC (H.265)の場合、NALタイプ32はVPS、33はSPS、34はPPS
                    // 32, 33, 34 -> {
                    //     Log.d(TAG, "HEVC NALタイプ: $nalType が見つかりました。現在はH.264を想定しています。")
                    //     csd.add(nalUnit.asReadOnlyBuffer())
                    // }
                }
                initialNalUnits.add(nalUnit) // 初期NALユニットを保持し、後で再フィードする

                if (spsBuffer != null && ppsBuffer != null) {
                    foundSpsPps = true
                }
            }

            if (spsBuffer == null || ppsBuffer == null) {
                Log.e(TAG, "最初の $maxInitialNalUnitsToScan NALユニットでSPSまたはPPSが見つかりませんでした。ビデオトラックを設定できません。")
                throw IOException("ビデオトラック設定のためのSPS/PPS NALユニットがありません。")
            }

            // SPS/PPSからMediaFormatを作成する
            // SPSをパースして実際の幅と高さを取得することが推奨されますが、ここではデフォルト値を使用します。
            val videoFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, DEFAULT_WIDTH, DEFAULT_HEIGHT).apply {
                setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL_SECONDS) // Key frame every X seconds

                // コーデック固有のデータを追加 (H.264のSPSとPPS)
                if (csd.isNotEmpty()) {
                    for (i in csd.indices) {
                        setByteBuffer("csd-$i", csd[i])
                    }
                }
            }

            try
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                {
                    videoFormat.keys.forEach { Log.v(TAG, "KEY : $it") }
                    videoFormat.features.forEach { Log.v(TAG, "FEATURE : $it") }
                }
                videoTrackIndex = muxer.addTrack(videoFormat)
            }
            catch (t: Throwable)
            {
                t.printStackTrace()
            }
            muxer.start()
            Log.d(TAG, "MediaMuxerが開始されました。ビデオトラックインデックス: $videoTrackIndex")
            listener?.onProgress(0)

            // 5. NALユニットをMediaMuxerに書き込む
            val bufferInfo = MediaCodec.BufferInfo()
            var frameCount = 0L
            val usPerFrame = TimeUnit.SECONDS.toMicros(1) / FRAME_RATE

            // 初期NALユニット (SPS, PPS、およびその他の初期NALなど) を再フィードする
            // MediaMuxerはCSDとして与えられたSPS/PPSとは別に、ストリーム内に含まれるSPS/PPSも処理できます。
            for (buf in initialNalUnits) {
                bufferInfo.offset = buf.position()
                bufferInfo.size = buf.remaining()

                val nalTypeByteIndex = startCodeLength(buf) ?: 0
                val nalType = (buf.get(nalTypeByteIndex) and 0x1F).toInt()
                val isKeyFrame = (nalType == 5) // IDR (Instantaneous Decoding Refresh)はキーフレーム

                bufferInfo.flags = if (isKeyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                bufferInfo.presentationTimeUs = presentationTimeUs
                muxer.writeSampleData(videoTrackIndex, buf, bufferInfo)
                Log.d(TAG, "初期NALユニット (タイプ: $nalType, キーフレーム: $isKeyFrame) をタイムスタンプ: $presentationTimeUs us で書き込みました")

                // SPS/PPSなどの設定NALユニットではタイムスタンプをインクリメントしないことが一般的です。
                // 実際のビデオフレーム（NALタイプ1や5など）に対してのみインクリメントします。
                // ただし、ストリーム内の全てのNALユニットに一貫したタイムスタンプを提供することも可能です。
                if (nalType == 1 || nalType == 5) { // スライスまたはIDRフレームの場合のみタイムスタンプを進める
                    presentationTimeUs += usPerFrame
                    frameCount++
                }
            }

            // 残りのNALユニットを処理し続ける
            while (true) {
                nalUnit = nalExtractor.readNextNalUnit()
                if (nalUnit == null) {
                    Log.d(TAG, "NALファイルの終端。")
                    break
                }

                val nalTypeByteIndex = startCodeLength(nalUnit) ?: 0
                val nalType = (nalUnit.get(nalTypeByteIndex) and 0x1F).toInt() // H.264 NALユニットタイプ
                val isKeyFrame = (nalType == 5) // IDR (Instantaneous Decoding Refresh)はキーフレーム

                bufferInfo.offset = nalUnit.position()
                bufferInfo.size = nalUnit.remaining()
                bufferInfo.flags = if (isKeyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                bufferInfo.presentationTimeUs = presentationTimeUs
                muxer.writeSampleData(videoTrackIndex, nalUnit, bufferInfo)
                Log.d(TAG, "NALユニット (タイプ: $nalType, キーフレーム: $isKeyFrame) をタイムスタンプ: $presentationTimeUs us、サイズ: ${nalUnit.remaining()} で書き込みました")

                // 次のフレームのためにプレゼンテーションタイムスタンプをインクリメントする
                // NALタイプ1 (非IDRスライス) と 5 (IDRスライス) は通常のビデオフレームです。
                if (nalType == 1 || nalType == 5) {
                    presentationTimeUs += usPerFrame
                    frameCount++
                }
                // 大まかな進捗率の推定
                listener?.onProgress(((frameCount * 100) / (nalInputFile.length() / (usPerFrame.toDouble() / 1000000 * FRAME_RATE))).toInt())
            }

            muxer.stop()
            muxer.release()
            Log.d(TAG, "MediaMuxerが停止され、解放されました。")

            // 6. MediaStoreエントリをペンディング状態から解除する
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentUri.let { uri ->
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, false) // ペンディングフラグを解除
                    resolver.update(uri, contentValues, null, null)
                }
            }
            listener?.onConversionComplete(contentUri)
            Log.i(TAG, "MP4変換成功！保存先: $contentUri")

        } catch (e: Exception) {
            try {
                Log.e(TAG, "MP4変換に失敗しました: ${e.message}", e)
                listener?.onConversionFailed(e)
                // 失敗した場合、MediaStoreエントリをクリーンアップする（推奨）
                val resolver = context.contentResolver
                val contentValues = ContentValues()
                contentUri?.let { uri ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val resolver = context.contentResolver
                        contentValues.clear()
                        contentValues.put(
                            MediaStore.MediaColumns.IS_PENDING,
                            0
                        ) // ペンディングフラグを解除 (念のため)
                        resolver.update(uri, contentValues, null, null)
                    }
                    resolver.delete(uri, null, null) // 不完全なファイルを削除
                }
            }
            catch (ee: Exception)
            {
                ee.printStackTrace()
            }
        } finally {
            try {
                nalExtractor?.close()
                // MediaMuxerにFileDescriptorを渡した場合、OutputStreamは直接Muxerの出力には関与しない。
                // しかし、取得したOutputStream自体はクローズする必要がある。
                // outputStream?.close()
            }
            catch (e: IOException)
            {
                Log.e(TAG, "error in the close. ${e.message}", e)
            }
            muxer?.release() // stop()が失敗した場合でもmuxerが解放されるようにする
        }
    }

    /**
     * ByteBufferの現在の位置から始まるNALユニットのスタートコードの長さを判断するヘルパー関数。
     * @param buffer NALユニットを含むByteBuffer。
     * @return スタートコードの長さ (3または4)、またはスタートコードが見つからない場合はnull。
     */
    private fun startCodeLength(buffer: ByteBuffer): Int? {
        if (buffer.remaining() < 4) return null // 4バイト未満の場合、4バイトのスタートコードは不可能

        val position = buffer.position()
        // 4バイトのスタートコード (0x00000001) をチェック
        if (buffer.get(position) == 0x00.toByte() &&
            buffer.get(position + 1) == 0x00.toByte() &&
            buffer.get(position + 2) == 0x00.toByte() &&
            buffer.get(position + 3) == 0x01.toByte()
        ) {
            return 4
        }
        // 3バイトのスタートコード (0x000001) をチェック
        else if (buffer.get(position) == 0x00.toByte() &&
            buffer.get(position + 1) == 0x00.toByte() &&
            buffer.get(position + 2) == 0x01.toByte()
        ) {
            return 3
        } else {
            return null // 現在の位置でスタートコードが見つからない
        }
    }

    /**
     * 変換イベントのためのインターフェース。
     */
    interface ConversionListener
    {
        fun onProgress(progress: Int)
        fun onConversionComplete(outputUri: Uri?)
        fun onConversionFailed(e: Exception)
    }

}
