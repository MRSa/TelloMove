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
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

class NALToMP4Converter3(private val context: Context, private val width: Int = VIDEO_WIDTH, private val height: Int = VIDEO_HEIGHT)
{
    private lateinit var decoder: MediaCodec
    private lateinit var encoder: MediaCodec
    private lateinit var muxer: MediaMuxer
    private var videoTrackIndex = -1
    private var isMuxerStarted = false

    private val decodedFramesQueue: BlockingQueue<ByteBuffer> = ArrayBlockingQueue(10)
    private val frameTimestampsQueue: BlockingQueue<Long> = ArrayBlockingQueue(10)

    private fun findNextNalStart(buffer: ByteArray, offset: Int, limit: Int): Int
    {
        for (i in offset until limit - 3) {
            if (buffer[i] == 0x00.toByte() && buffer[i + 1] == 0x00.toByte())
            {
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

    private fun prepareOutputFile(outputFileName : String, outputDir :String = "TelloMove"): Uri?
    {
        var outputFileUri : Uri? = null
        try
        {
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
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
        return (outputFileUri)
    }

    private fun initializeDecoder()
    {
        try
        {
            Log.v(TAG, "initializeDecoder()")
            if (!::decoder.isInitialized)
            {
                decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            }
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            decoder.configure(format, null, null, 0)
            decoder.start()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    fun convertNALToMp4(inputFileName: String, outputFileName: String, outputDir :String = "TelloMove")
    {
        var outputUri : Uri? = null
        var fileInputStream: FileInputStream? = null
        try
        {
            initializeDecoder()
            outputUri = prepareOutputFile(outputFileName)
            if (outputUri == null)
            {
                Log.v(TAG, "outputUri is Null...")
                throw IOException("outputUri is null.")
            }
            val outputPfd = context.contentResolver.openFileDescriptor(outputUri, "w")
            if (outputPfd == null)
            {
                Log.v(TAG, " ")
                throw IOException("outputPfd is null.")
            }
            muxer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                MediaMuxer(outputPfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            } else {
                MediaMuxer("${Environment.DIRECTORY_MOVIES}/$outputDir/$outputFileName", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            }
            outputPfd.close()


            // 5. 処理を開始: NAL読み込み -> デコード -> エンコード -> 多重化
            val baseDir = context.getExternalFilesDir(null)
            val filePath = "${baseDir?.absolutePath}/$outputDir/$inputFileName"
            fileInputStream = FileInputStream(File(filePath))
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
                        while (nalStart < totalBytes) {
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
                            val inBufferId = decoder.dequeueInputBuffer(TIMEOUT_MS) ?: -1 // 10ms タイムアウト
                            if (inBufferId >= 0) {
                                val buffer = decoder.getInputBuffer(inBufferId)
                                buffer?.clear()
                                buffer?.put(nalUnit)
                                // NALにタイムスタンプ情報がないため、フレームレートに基づいて推測値を付与
                                decoder.queueInputBuffer(inBufferId, 0, nalUnit.limit(), presentationTimeUs, 0)
                                presentationTimeUs += (1_000_000 / VIDEO_FRAME_RATE).toLong() // マイクロ秒単位
                            } else {
                                // デコーダの入力バッファが利用できない場合、少し待つ
                                Thread.sleep(10)
                            }
                            nalStart = nalEnd
                        }
                        if (nalStart == totalBytes) { // 現在のバッファからすべてのNALを処理済み
                            bufferOffset = 0
                        }
                    }
                    // ストリームの終端をデコーダに通知
                    val inBufferId = decoder.dequeueInputBuffer(TIMEOUT_MS) ?: -1
                    if (inBufferId >= 0) {
                        decoder.queueInputBuffer(inBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    }
                }
                catch (e: Exception)
                {
                    e.printStackTrace()
                }
            }

            // デコーダ出力 & エンコーダ入力スレッド
            val decoderOutputThread = Thread {
                val bufferInfo = MediaCodec.BufferInfo()
                var decoderOutputDone = false
                while (!decoderOutputDone) {
                    val outBufferId = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_MS) ?: -1
                    when (outBufferId) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            // デコーダの出力フォーマットが変更された場合 (稀だが考慮)
                            Log.i(TAG, "INF>OUTPUT FORMAT CHANGED ${decoder.outputFormat}")
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            // 利用可能な出力バッファがない場合
                        }
                        else -> {
                            if (outBufferId >= 0) {
                                val outputBuffer = decoder.getOutputBuffer(outBufferId)
                                if (outputBuffer != null && bufferInfo.size > 0) {
                                    // デコードされたraw YUVフレームを新しいByteBufferにコピーし、キューに投入
                                    val copyBuffer = ByteBuffer.allocateDirect(bufferInfo.size)
                                    copyBuffer.put(outputBuffer)
                                    copyBuffer.flip()
                                    decodedFramesQueue.put(copyBuffer)
                                    frameTimestampsQueue.put(bufferInfo.presentationTimeUs)
                                }
                                decoder.releaseOutputBuffer(outBufferId, false)
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
                    val outBufferId = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_MS) ?: -1
                    when (outBufferId) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            // エンコーダの出力フォーマットが変更された場合
                            val newFormat = encoder.outputFormat
                            Log.i(TAG, "エンコーダの出力フォーマットが変更されました: $newFormat")
                            if (muxer != null) {
                                videoTrackIndex = muxer.addTrack(newFormat)
                                muxer.start() // Muxerを開始
                                isMuxerStarted = true
                            }
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            // 利用可能な出力バッファがない場合
                        }
                        else -> {
                            if (outBufferId >= 0) {
                                val outputBuffer = encoder.getOutputBuffer(outBufferId)
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
                                encoder.releaseOutputBuffer(outBufferId, false)
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
                            val inBufferId = encoder.dequeueInputBuffer(TIMEOUT_MS) ?: -1
                            if (inBufferId >= 0) {
                                // エンコーダにストリームの終端を通知
                                encoder.queueInputBuffer(inBufferId, 0, 0, lastFrameTimestamp, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            }
                            break
                        }
                        val inBufferId = encoder.dequeueInputBuffer(TIMEOUT_MS) ?: -1
                        if (inBufferId >= 0) {
                            val inputBuffer0 = encoder.getInputBuffer(inBufferId)
                            inputBuffer0?.clear()
                            inputBuffer0?.put(decodedFrame) // デコードされたYUVフレームを投入
                            encoder.queueInputBuffer(inBufferId, 0, decodedFrame.limit(), frameTimestamp, 0)
                        } else {
                            // エンコーダの入力バッファが利用できない場合、フレームをキューに戻し、少し待つ
                            decodedFramesQueue.put(decodedFrame)
                            frameTimestampsQueue.put(frameTimestamp)
                            Thread.sleep(10)
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
            // return@withContext outputFileUri

        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
        finally
        {
            if (outputUri != null)
            {
                try
                {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, outputFileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$outputDir/")
                            put(MediaStore.Video.Media.IS_PENDING, false)
                        }
                    }
                    context.contentResolver.update(outputUri, contentValues, null, null)
                }
                catch (ex: Exception)
                {
                    ex.printStackTrace()
                }
            }
        }
    }


    companion object
    {
        private val TAG = NALToMP4Converter3::class.java.simpleName
        private const val BUFFER_SIZE = 1024 * 1024 + 16
        private const val VIDEO_WIDTH = 960
        private const val VIDEO_HEIGHT = 720
        private const val VIDEO_FRAME_RATE = 30
        private const val TIMEOUT_MS = 10000L
    }
}