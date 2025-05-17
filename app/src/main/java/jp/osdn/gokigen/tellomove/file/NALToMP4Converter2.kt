package jp.osdn.gokigen.tellomove.file

import android.content.ContentValues
import android.content.Context
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import jp.osdn.gokigen.tellomove.communication.IBitmapReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit


class NALToMP4Converter2(private val context: Context)
{
    private var decoder: MediaCodec? = null
    private var encoder: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var videoTrackIndex = -1
    private var isMuxerStarted = false

    private val decodedFramesQueue: BlockingQueue<ByteBuffer> = ArrayBlockingQueue(10)
    private val frameTimestampsQueue: BlockingQueue<Long> = ArrayBlockingQueue(10)

    /**
     * NAL形式の動画ファイルを読み込み、MP4形式に変換して保存します。
     *
     * @return 変換されたMP4ファイルのUri、または変換失敗の場合はnull
     */
    suspend fun convertNALToMp4(inputFileName: String, outputFileName: String, outputDir :String = "TelloMove", bitmapNotify: IBitmapReceiver? = null): Uri? = withContext(Dispatchers.IO)
    {
        var outputFileUri: Uri? = null
        var outputPfd: ParcelFileDescriptor? = null
        var fileInputStream: FileInputStream? = null

        try
        {
            // 1. MediaStoreを介して出力MP4ファイルを作成
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, outputFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$outputDir/")
                    put(MediaStore.Video.Media.IS_PENDING, true)
                }
            }
            outputFileUri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            if (outputFileUri == null)
            {
                throw IOException("新しい MediaStore ファイルの作成に失敗しました。")
            }

            outputPfd = context.contentResolver.openFileDescriptor(outputFileUri, "w")
            if (outputPfd == null)
            {
                throw IOException("出力ファイルの ParcelFileDescriptor のオープンに失敗しました。")
            }

            muxer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                //MediaMuxer(pfd.fileDescriptor, MediaMuxer.OutputFormat.MPEG_4)
                MediaMuxer(outputPfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            }
            else
            {
                // API 26未満では、MediaMuxer.OutputFormatはint型
                // MediaMuxer(pfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG4)
                MediaMuxer("${Environment.DIRECTORY_MOVIES}/$outputDir/$outputFileName", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            }
            //outputPfd.close() // MediaMuxerに渡した後、ParcelFileDescriptorをクローズする
            // muxer = MediaMuxer(outputPfd.fileDescriptor, MediaMuxer.OutputFormat.MPEG_4)


            // 3. MediaCodec デコーダを初期化 (入力NALユニット用)
            // NALファイルのビデオコーデックタイプ (例: H.264/AVC) を決定します。
            // ここではAVCと仮定しています。実際のアプリでは、NALユニットを検査して特定する必要があります。
            val inputFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, VIDEO_WIDTH, VIDEO_HEIGHT)
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            // 出力サーフェスは不要 (rawバッファにデコードするため)
            decoder?.configure(inputFormat, null, null, 0)
            decoder?.start()

            // 4. MediaCodec エンコーダを初期化 (MP4出力用)
            val outputFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, VIDEO_WIDTH, VIDEO_HEIGHT).apply {
                // エンコーダの入力カラーフォーマット。デコーダの出力と互換性のあるものを選びます。
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BITRATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_I_FRAME_INTERVAL)
            }
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder?.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder?.start()

            // 5. 処理を開始: NAL読み込み -> デコード -> エンコード -> 多重化
            val baseDir = context.getExternalFilesDir(null)
            val filePath = "${baseDir?.absolutePath}/$outputDir/$inputFileName"
            fileInputStream = FileInputStream(File(filePath))

            // NAL読み込み用の十分な大きさのバッファ
            val inputBuffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            var presentationTimeUs: Long = 0

            // デコーダ入力スレッド
            val decoderInputThread = Thread {
                var bufferOffset = 0
                try
                {
                    // ファイルからデータを読み込み、NALユニットをデコーダに投入
                    while (fileInputStream.read(inputBuffer, bufferOffset, inputBuffer.size - bufferOffset).also { bytesRead = it } != -1 || bufferOffset > 0) {
                        val totalBytes = bufferOffset + bytesRead
                        var nalStart = 0
                        while (nalStart < totalBytes)
                        {
                            // 次のNALユニットの開始位置を見つける
                            val nalEnd = findNextNalStart(inputBuffer, nalStart, totalBytes)
                            if (nalEnd == -1) { // このバッファにこれ以上NALがない場合、残りを保存
                                System.arraycopy(inputBuffer, nalStart, inputBuffer, 0, totalBytes - nalStart)
                                bufferOffset = totalBytes - nalStart
                                break
                            }
                            val nalUnitSize = nalEnd - nalStart

                            // NALユニットを新しいByteBufferにコピー
                            val nalUnit = ByteBuffer.allocateDirect(nalUnitSize)
                            nalUnit.put(inputBuffer, nalStart, nalUnitSize)
                            nalUnit.flip()

                            // デコーダにNALユニットを投入
                            val inBufferId = decoder?.dequeueInputBuffer(TIMEOUT_US) ?: -1 // 10ms タイムアウト
                            if (inBufferId >= 0)
                            {
                                val buffer = decoder?.getInputBuffer(inBufferId)
                                buffer?.clear()
                                buffer?.put(nalUnit)
                                // NALにタイムスタンプ情報がないため、フレームレートに基づいて推測値を付与
                                decoder?.queueInputBuffer(inBufferId, 0, nalUnit.limit(), presentationTimeUs, 0)
                                presentationTimeUs += (VIDEO_BITRATE / VIDEO_FRAME_RATE).toLong()
                            }
                            else
                            {
                                // デコーダの入力バッファが利用できない場合、少し待つ
                                Thread.sleep(WAIT_MS)
                            }
                            nalStart = nalEnd
                        }
                        if (nalStart == totalBytes)
                        {
                            // 現在のバッファからすべてのNALを処理済み
                            bufferOffset = 0
                        }
                    }
                    // ストリームの終端をデコーダに通知
                    val inBufferId = decoder?.dequeueInputBuffer(TIMEOUT_US) ?: -1
                    if (inBufferId >= 0)
                    {
                        decoder?.queueInputBuffer(inBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    }
                }
                catch (e: Exception)
                {
                    Log.e(TAG, "デコーダ入力スレッドのエラー", e)
                }
            }

            // デコーダ出力 & エンコーダ入力スレッド
            val decoderOutputThread = Thread {
                val bufferInfo = MediaCodec.BufferInfo()
                var decoderOutputDone = false
                while (!decoderOutputDone)
                {
                    val outBufferId = decoder?.dequeueOutputBuffer(bufferInfo, TIMEOUT_US) ?: -1
                    when (outBufferId)
                    {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            // デコーダの出力フォーマットが変更された場合 (稀だが考慮)
                            Log.i(TAG, "デコーダの出力フォーマットが変更されました: ${decoder?.outputFormat}")
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            // 利用可能な出力バッファがない場合
                        }
                        else -> {
                            if (outBufferId >= 0) {
                                val outputBuffer = decoder?.getOutputBuffer(outBufferId)
                                if (outputBuffer != null && bufferInfo.size > 0) {
                                    // デコードされたraw YUVフレームを新しいByteBufferにコピーし、キューに投入
                                    val copyBuffer = ByteBuffer.allocateDirect(bufferInfo.size)
                                    copyBuffer.put(outputBuffer)
                                    copyBuffer.flip()
                                    decodedFramesQueue.put(copyBuffer)
                                    frameTimestampsQueue.put(bufferInfo.presentationTimeUs)
                                }
                                decoder?.releaseOutputBuffer(outBufferId, false)
                                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                    decoderOutputDone = true
                                }
                            }
                        }
                    }
                }
                // エンコーダにストリームの終端を通知するためのセンチネル (番兵) をキューに投入
                try {
                    decodedFramesQueue.put(ByteBuffer.allocate(0)) // EOFセンチネル
                    frameTimestampsQueue.put(-1L) // EOFセンチネル
                } catch (e: InterruptedException) { /* ignore */ }
            }

            // エンコーダ出力 & 多重化入力スレッド
            val encoderOutputThread = Thread {
                val bufferInfo = MediaCodec.BufferInfo()
                var encoderOutputDone = false
                var lastFrameTimestamp: Long = 0

                while (!encoderOutputDone) {
                    // エンコーダからエンコード済みデータを取得し、Muxerに書き込む
                    val outBufferId = encoder?.dequeueOutputBuffer(bufferInfo, TIMEOUT_US) ?: -1
                    when (outBufferId) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            // エンコーダの出力フォーマットが変更された場合
                            val newFormat = encoder?.outputFormat
                            Log.i(TAG, "エンコーダの出力フォーマットが変更されました: $newFormat")
                            if (muxer != null) {
                                videoTrackIndex = muxer!!.addTrack(newFormat!!)
                                muxer!!.start() // Muxerを開始
                                isMuxerStarted = true
                            }
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            // 利用可能な出力バッファがない場合
                        }
                        else -> {
                            if (outBufferId >= 0) {
                                val outputBuffer = encoder?.getOutputBuffer(outBufferId)
                                if (outputBuffer != null && bufferInfo.size > 0) {
                                    if (!isMuxerStarted) {
                                        // Muxerがまだ開始されていない場合 (通常はINFO_OUTPUT_FORMAT_CHANGEDで開始されるべき)
                                        Thread.sleep(10)
                                        continue
                                    }
                                    outputBuffer.position(bufferInfo.offset)
                                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                    muxer?.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                                    lastFrameTimestamp = bufferInfo.presentationTimeUs
                                }
                                encoder?.releaseOutputBuffer(outBufferId, false)
                                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                    encoderOutputDone = true
                                }
                            }
                        }
                    }

                    // デコードされたフレームをキューから取得し、エンコーダに投入
                    val decodedFrame = decodedFramesQueue.poll(10, TimeUnit.MILLISECONDS)
                    val frameTimestamp = frameTimestampsQueue.poll(10, TimeUnit.MILLISECONDS)

                    if (decodedFrame != null && frameTimestamp != null) {
                        if (frameTimestamp == -1L) { // EOFセンチネルの場合
                            val inBufferId = encoder?.dequeueInputBuffer(TIMEOUT_US) ?: -1
                            if (inBufferId >= 0) {
                                // エンコーダにストリームの終端を通知
                                encoder?.queueInputBuffer(inBufferId, 0, 0, lastFrameTimestamp, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            }
                            break
                        }
                        val inBufferId = encoder?.dequeueInputBuffer(TIMEOUT_US) ?: -1
                        if (inBufferId >= 0) {
                            val inputBuffer0 = encoder?.getInputBuffer(inBufferId)
                            inputBuffer0?.clear()
                            inputBuffer0?.put(decodedFrame) // デコードされたYUVフレームを投入
                            encoder?.queueInputBuffer(inBufferId, 0, decodedFrame.limit(), frameTimestamp, 0)
                        } else {
                            // エンコーダの入力バッファが利用できない場合、フレームをキューに戻し、少し待つ
                            Log.v(TAG,"WAIT...")
                            decodedFramesQueue.put(decodedFrame)
                            frameTimestampsQueue.put(frameTimestamp)
                            Thread.sleep(WAIT_MS)
                        }
                    }
                }
            }

            // 各スレッドを開始
            decoderInputThread.start()
            decoderOutputThread.start()
            encoderOutputThread.start()

            // 各スレッドの終了を待つ
            decoderInputThread.join()
            decoderOutputThread.join()
            encoderOutputThread.join()

            Log.i(TAG, "NAL to MP4 変換が完了しました。")
            return@withContext outputFileUri

        } catch (e: Exception) {
            e.printStackTrace()
            outputFileUri?.let { uri ->
                // エラー発生時に部分的に作成されたファイルを削除
                context.contentResolver.delete(uri, null, null)
            }
            return@withContext null
        } finally {
            // ----- 書き込み完了
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                val contentValues = ContentValues()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, false)
                if (outputFileUri != null)
                {
                    context.contentResolver.update(outputFileUri, contentValues, null, null)
                }
            }

            // リソースの解放
            decoder?.stop()
            decoder?.release()
            encoder?.stop()
            encoder?.release()
            if (muxer != null && isMuxerStarted)
            {
                try
                {
                    muxer?.stop() // Muxerを停止
                }
                catch (e: Exception)
                {
                    e.printStackTrace()
                }
            }
            muxer?.release()
            outputPfd?.close()
            fileInputStream?.close()
            Log.d(TAG, "リソースが解放されました。")
        }
    }

    /**
     * 次のNALユニットの開始位置を検索するヘルパー関数。
     * NALユニットは通常、0x00 0x00 0x00 0x01 または 0x00 0x00 0x01 で始まります。
     *
     * @param buffer NALデータを含むバイト配列。
     * @param offset 検索を開始するオフセット。
     * @param limit 検索を終了する最大インデックス。
     * @return 次のNALユニットの開始位置のインデックス、または制限内で見つからない場合は-1。
     */
    private fun findNextNalStart(buffer: ByteArray, offset: Int, limit: Int): Int {
        for (i in offset until limit - 3) {
            if (buffer[i] == 0x00.toByte() && buffer[i + 1] == 0x00.toByte()) {
                if (buffer[i + 2] == 0x01.toByte()) { // 00 00 01
                    return i + 3
                }
                if (i + 3 < limit && buffer[i + 2] == 0x00.toByte() && buffer[i + 3] == 0x01.toByte()) { // 00 00 00 01
                    return i + 4
                }
            }
        }
        return -1
    }

    companion object
    {
        private val TAG = NALToMP4Converter2::class.java.simpleName
        private const val BUFFER_SIZE = 1024 * 1024 + 16
        private const val VIDEO_WIDTH = 960
        private const val VIDEO_HEIGHT = 720
        private const val VIDEO_FRAME_RATE = 15
        private const val TIMEOUT_US = 10000L
        private const val WAIT_MS = 10L
        private const val VIDEO_BITRATE = 1_000_000
        private const val VIDEO_I_FRAME_INTERVAL = 1
    }
}
