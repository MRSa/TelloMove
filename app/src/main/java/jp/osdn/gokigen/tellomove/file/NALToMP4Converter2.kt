package jp.osdn.gokigen.tellomove.file

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.core.graphics.createBitmap
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
    private lateinit var muxer: MediaMuxer
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
            //muxer = MediaMuxer(outputPfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

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
            val fileSize = fileInputStream.available()
            val bufferSize = fileSize // BUFFER_SIZE
            val inputBuffer = ByteArray(bufferSize)
            var bytesRead: Int
            var presentationTimeUs: Long = 0

            // デコーダ入力スレッド (改善検討版）
            val decoderInputThread = Thread {
                var nalCount = 0
                var fileOffset = 0
                var positionOffset = 0
                //val fileSize = fileInputStream.available()
                var checkFinished = false
                try
                {
                    while ((!checkFinished)&&(fileOffset < fileSize))
                    {
                        Log.v(TAG, "[decoder dequeue INPUT buffer]")
                        var readStartPosition = 0
                        val readLength = fileInputStream.read(inputBuffer, positionOffset, (bufferSize - positionOffset))
                        if (readLength < 0)
                        {
                            Log.v(TAG, "READ FAILURE...")
                            break
                        }
                        positionOffset = 0
                        while ((!checkFinished)&&(readStartPosition >= 0)&&(readStartPosition < readLength))
                        {
                            val nalStartPosition = findNextNalStart(inputBuffer, readStartPosition, readLength)
                            val nalNextPosition = if (nalStartPosition >= 0) {
                                findNextNalStart(inputBuffer, (nalStartPosition + 3), readLength)
                            } else {
                                -1
                            }
                            // Log.v(TAG, "::::::::::  nalStartPosition :$nalStartPosition  nalNextPosition :$nalNextPosition  readStartPosition:$readStartPosition  fileSize: $fileSize  read: $readLength ($fileOffset)")
                            if (nalNextPosition > 0)
                            {
                                val dataSize = (nalNextPosition - nalStartPosition)
                                val nalUnit = ByteBuffer.allocateDirect(dataSize)
                                nalUnit.put(inputBuffer, nalStartPosition, dataSize)
                                nalUnit.flip()

                                // デコーダにNALユニットを入れる
                                var  inBufferId : Int
                                do {
                                    inBufferId = decoder?.dequeueInputBuffer(TIMEOUT_US) ?: MediaCodec.INFO_TRY_AGAIN_LATER
                                    Thread.sleep(WAIT_MS)
                                } while (inBufferId == MediaCodec.INFO_TRY_AGAIN_LATER)

                                nalCount++
                                //Log.v(TAG, "PUT into the decoder input buffer (id: $inBufferId)")
                                val buffer = decoder?.getInputBuffer(inBufferId)
                                buffer?.clear()
                                buffer?.put(nalUnit)
                                decoder?.queueInputBuffer(inBufferId, 0, nalUnit.limit(), presentationTimeUs, 0)
                                presentationTimeUs += (VIDEO_BITRATE / VIDEO_FRAME_RATE).toLong()
                            }
                            else
                            {
                                // ---------- 残りバイト数
                                val remainPercent = (fileOffset.toFloat() / fileSize.toFloat() * 100.0f).toInt()
                                //positionOffset = readLength - readStartPosition
                                Log.v(TAG, "DATA REMAIN BYTES : read:$readLength  pos:$nalStartPosition, rest position:$positionOffset (${readLength - readStartPosition} bytes.) READ: $remainPercent % SIZE: $fileSize ")
                                // ----- 読み込みデータの先頭に残りデータを詰めておく -----
                                //System.arraycopy(inputBuffer, nalStartPosition,  inputBuffer, 0, positionOffset)

                                //  とりあえず break
                                checkFinished = true
                            }
                            // ---- 読み込みポジションをすすめる。
                            readStartPosition = nalNextPosition + 3
                        }
                        fileOffset += readLength
                    }
                }
                catch (e: Exception)
                {
                    e.printStackTrace()
                }
                finally
                {
                    try
                    {
                        // ストリームの終端をデコーダに通知
                        Log.v(TAG, "Write Termination... (nalCount: $nalCount)")
                        var inBufferId : Int
                        do {
                            inBufferId = decoder?.dequeueInputBuffer(TIMEOUT_US) ?: MediaCodec.INFO_TRY_AGAIN_LATER
                        } while (inBufferId == MediaCodec.INFO_TRY_AGAIN_LATER)
                        if (inBufferId >= 0)
                        {
                            decoder?.queueInputBuffer(inBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        }
                        Log.v(TAG, " DONE...decoder queue finished...")
                    }
                    catch (ex: Exception)
                    {
                        ex.printStackTrace()
                    }
                    Log.v(TAG, " ====== STREAM DATA READ FINISHED : $fileSize bytes.")
                }
            }

            // デコーダ入力スレッド
            val decoderInputThreadOriginal = Thread {
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
                var decodeCount = 0
                var decoderOutputDone = false
                while (!decoderOutputDone)
                {
                    Log.v(TAG, "[decoder dequeue output buffer]")
                    var outBufferId = MediaCodec.INFO_TRY_AGAIN_LATER
                    while (outBufferId == MediaCodec.INFO_TRY_AGAIN_LATER)
                    {
                        try
                        {
                            outBufferId = decoder?.dequeueOutputBuffer(bufferInfo, TIMEOUT_US) ?: MediaCodec.INFO_TRY_AGAIN_LATER
                            Thread.sleep(WAIT_MS)
                        }
                        catch (e: Exception)
                        {
                            e.printStackTrace()
                            //try
                            //{
                                //val inputFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, VIDEO_WIDTH, VIDEO_HEIGHT)
                                //decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                                //decoder?.configure(inputFormat, null, null, 0)
                                //decoder?.start()
                                //Thread.sleep(WAIT_MS)
                                //Log.v(TAG, "DECODER RESTARTED")
                            //}
                            //catch (ex: Exception)
                            //{
                            //    ex.printStackTrace()
                            //}
                        }
                    }
                    Log.v(TAG, "DECODER dequeue output buffer: outBufferId: $outBufferId")
                    when (outBufferId)
                    {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            // デコーダの出力フォーマットが変更された場合 (稀だが考慮)
                            Log.i(TAG, "デコーダの出力フォーマットが変更されました: ${decoder?.outputFormat}")





                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            // 利用可能な出力バッファがない場合
                            Log.v(TAG, "[MediaCodec.INFO_TRY_AGAIN_LATER] ($decodeCount)")
                            try
                            {
                                // 試しに止めてみる...
                                Thread.sleep(100)
                            }
                            catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                        else -> {
                            Log.v(TAG, "TYPE: $outBufferId  (count: $decodeCount)")
                            if (outBufferId >= 0) {
                                val outputBuffer = decoder?.getOutputBuffer(outBufferId)
                                if (outputBuffer != null && bufferInfo.size > 0) {
                                    // デコードされたraw YUVフレームを新しいByteBufferにコピーし、キューに投入
                                    val copyBuffer = ByteBuffer.allocateDirect(bufferInfo.size)
                                    copyBuffer.put(outputBuffer)
                                    copyBuffer.flip()
                                    decodedFramesQueue.put(copyBuffer)
                                    frameTimestampsQueue.put(bufferInfo.presentationTimeUs)
                                    decodeCount++
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
                    decodeCount++
                } catch (e: InterruptedException) { /* ignore */ }
                Log.v(TAG, "DECODE OUTPUT THREAD FINISHED (count: $decodeCount)")
            }

            // エンコーダ出力 & 多重化入力スレッド
            val encoderOutputThread = Thread {
                val bufferInfo = MediaCodec.BufferInfo()
                var encoderOutputDone = false
                var lastFrameTimestamp: Long = 0

                while (!encoderOutputDone) {
                    // エンコーダからエンコード済みデータを取得し、Muxerに書き込む
                    var outBufferId = MediaCodec.INFO_TRY_AGAIN_LATER
                    do {
                        try
                        {
                            outBufferId = encoder?.dequeueOutputBuffer(bufferInfo, TIMEOUT_US) ?: MediaCodec.INFO_TRY_AGAIN_LATER
                            Thread.sleep(WAIT_MS)
                        }
                        catch (e: Exception)
                        {
                            e.printStackTrace()
                        }
                    } while (outBufferId == MediaCodec.INFO_TRY_AGAIN_LATER)
                    // val outBufferId = encoder?.dequeueOutputBuffer(bufferInfo, TIMEOUT_US) ?: -1
////////////////////////////////////////////////////////////////////////////////
/*
                                            if (outBufferId >= 0)
                                            {
                                                // デコードされたデータをビットマップ化
                                                Log.v(TAG, "BITMAP outBufferId: $outBufferId, size: ${bufferInfo.size}")
                                                try
                                                {
                                                    val yuvBytes = ByteArray(VIDEO_WIDTH * VIDEO_HEIGHT * 3 / 2)
                                                    val outputBuffer = decoder?.getOutputBuffer(outBufferId)
                                                    outputBuffer?.position(bufferInfo.offset)
                                                    outputBuffer?.limit(bufferInfo.offset + bufferInfo.size)
                                                    outputBuffer?.get(yuvBytes)
                                                    val bitmap = yuv420ToBitmap(yuvBytes)
                                                    bitmapNotify?.updateBitmapImage(bitmap)
                                                }
                                                catch (ee: Exception)
                                                {
                                                    ee.printStackTrace()
                                                }
                                            }
*/
////////////////////////////////////////////////////////////////////////////////////
                    Log.v(TAG, "  outBufferId: $outBufferId")
                    when (outBufferId) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            // エンコーダの出力フォーマットが変更された場合
                            val newFormat = encoder?.outputFormat
                            Log.i(TAG, "エンコーダの出力フォーマットが変更されました: $newFormat")
                            if (::muxer.isInitialized)
                            {
                                videoTrackIndex = muxer.addTrack(newFormat!!)
                                muxer.start()
                                isMuxerStarted = true
                            }
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            // 利用可能な出力バッファがない場合
                            Log.v(TAG, "(MediaCodec.INFO_TRY_AGAIN_LATER)")
                            try
                            {
                                // 試しに止めてみる...
                                Thread.sleep(100)
                            }
                            catch (ex: Exception)
                            {
                                ex.printStackTrace()
                            }
                        }
                        else -> {
                            Log.v(TAG, "TYPE: $outBufferId  ")
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
                                    muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
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
            //decoderInputThread.start()
            decoderInputThreadOriginal.start()
            decoderOutputThread.start()
            encoderOutputThread.start()

            // 各スレッドの終了を待つ
            //decoderInputThread.join()
            decoderInputThreadOriginal.join()
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
            if (::muxer.isInitialized && isMuxerStarted)
            {
                try
                {
                    muxer.stop() // Muxerを停止
                }
                catch (e: Exception)
                {
                    e.printStackTrace()
                }
            }
            muxer.release()
            outputPfd?.close()
            fileInputStream?.close()
            Log.d(TAG, "Released resource")
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
                    return i + 3   // for decoderInputThreadOriginal
                    //return i          // for decoderInputThread
                }
                if (i + 3 < limit && buffer[i + 2] == 0x00.toByte() && buffer[i + 3] == 0x01.toByte()) { // 00 00 00 01
                    return i + 4   // for decoderInputThreadOriginal
                    //return i         // for decoderInputThread
                }
            }
        }
        return -1
    }

    private fun yuv420ToBitmap(yuvBytes: ByteArray, width: Int = VIDEO_WIDTH, height: Int = VIDEO_HEIGHT): Bitmap
    {
        val out = IntArray(width * height)
        val uIndex = width * height
        val vIndex = width * height * 5 / 4
        var yIndex = 0
        var i = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val yValue = (yuvBytes[yIndex++].toInt() and 0xff) - 16
                val uValue = (yuvBytes[uIndex + (x shr 1) + (y shr 1) * (width shr 1)].toInt() and 0xff) - 128
                val vValue = (yuvBytes[vIndex + (x shr 1) + (y shr 1) * (width shr 1)].toInt() and 0xff) - 128

                var r = (1.164 * yValue + 1.596 * vValue).toInt()
                var g = (1.164 * yValue - 0.391 * uValue - 0.813 * vValue).toInt()
                var b = (1.164 * yValue + 2.018 * uValue).toInt()

                r = r.coerceIn(0, 255)
                g = g.coerceIn(0, 255)
                b = b.coerceIn(0, 255)

                out[i++] = 0xff000000.toInt() or (r shl 16) or (g shl 8) or b
            }
        }
        val bitmap = createBitmap(width, height)
        bitmap.setPixels(out, 0, width, 0, 0, width, height)
        return bitmap
    }

    companion object
    {
        private val TAG = NALToMP4Converter2::class.java.simpleName
        private const val BUFFER_SIZE = 1024 * 1024 + 16
        private const val VIDEO_WIDTH = 960
        private const val VIDEO_HEIGHT = 720
        private const val VIDEO_FRAME_RATE = 15
        private const val TIMEOUT_US = 100000L
        private const val WAIT_MS = 50L // 15 -> xxx
        private const val VIDEO_BITRATE = 1_000_000
        private const val VIDEO_I_FRAME_INTERVAL = 1
    }
}
