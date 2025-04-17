package jp.osdn.gokigen.tellomove.communication

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import androidx.core.graphics.createBitmap
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/*
class StreamReceiver(private val streamPortNo: Int = 11111)
{
    private var isReceiving = false
    private lateinit var receiverSocket: DatagramSocket
    private lateinit var bitmapReceiver: IBitmapReceiver

    private var decoder: MediaCodec? = null
    private var inputBuffers: Array<ByteBuffer>? = null
    private var outputBuffers: Array<ByteBuffer>? = null
    private var outputFormat: MediaFormat? = null

    fun startReceive()
    {
        val buffer = ByteArray(BUFFER_SIZE)
        isReceiving = true
        try
        {
            Thread {
                try
                {
                    val format = MediaFormat.createVideoFormat(MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT)
                    decoder = MediaCodec.createDecoderByType(MIME_TYPE)
                    decoder?.configure(format, null, null, 0)
                    decoder?.start()
                    receiverSocket = DatagramSocket(streamPortNo)
                }
                catch (e: Exception)
                {
                    e.printStackTrace()
                }
                while (isReceiving)
                {
                    try
                    {
                        val receivePacket = DatagramPacket(buffer, buffer.size)
                        inputBuffers = decoder?.inputBuffers
                        outputBuffers = decoder?.outputBuffers
                        outputFormat = decoder?.outputFormat

                        if (::receiverSocket.isInitialized)
                        {
                            receiverSocket.soTimeout = TIMEOUT_MS
                            receiverSocket.receive(receivePacket)

                            val receivedData = receivePacket.data.copyOfRange(0, receivePacket.length)
                            try
                            {
                                val inputBufferId = decoder?.dequeueInputBuffer(10000) ?: -1
                                if (inputBufferId >= 0)
                                {
                                    val inputBuffer = inputBuffers?.get(inputBufferId)
                                    inputBuffer?.clear()
                                    inputBuffer?.put(receivedData)
                                    decoder?.queueInputBuffer(
                                        inputBufferId,
                                        0,
                                        receivedData.size,
                                        0,
                                        0
                                    )
                                }

                                val bufferInfo = MediaCodec.BufferInfo()
                                val outputBufferId = decoder?.dequeueOutputBuffer(bufferInfo, TIMEOUT_US) ?: -1
                                if (outputBufferId >= 0) {
                                    val outputBuffer = outputBuffers?.get(outputBufferId)
                                    outputBuffer?.position(bufferInfo.offset)
                                    outputBuffer?.limit(bufferInfo.offset + bufferInfo.size)

                                    // YUV データを Bitmap に変換
                                    if (outputFormat?.getInteger(MediaFormat.KEY_COLOR_FORMAT) ==
                                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
                                    ) {
                                        val yuvBytes = ByteArray(outputBuffer?.remaining()?: 0)
                                        outputBuffer?.get(yuvBytes)
                                        val bitmap = yuv420ToBitmap(
                                            yuvBytes,
                                            outputFormat?.getInteger(MediaFormat.KEY_WIDTH)?: VIDEO_WIDTH,
                                            outputFormat?.getInteger(MediaFormat.KEY_HEIGHT)?: VIDEO_HEIGHT
                                        )
                                        if (::bitmapReceiver.isInitialized)
                                        {
                                            Log.v(TAG, "UPDATE BITMAP")
                                            bitmapReceiver.updateBitmapImage(bitmap)
                                        }
                                    } else {
                                        Log.w(TAG, "Unsupported color format: ${outputFormat?.getInteger(MediaFormat.KEY_COLOR_FORMAT)}")
                                    }
                                    decoder?.releaseOutputBuffer(outputBufferId, false)
                                } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                    outputFormat = decoder?.outputFormat
                                    Log.i(TAG, "Output format changed: $outputFormat")
                                    outputBuffers = decoder?.outputBuffers
                                } else if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                    Log.d(TAG, "No output buffer available...")
                                }
                            }
                            catch (ee: Exception)
                            {
                                ee.printStackTrace()
                            }
                            //checkReceiveData(receivePacket)
                        }
                        else
                        {
                            Log.v(TAG, "receiveSocket is not initialized...")
                        }
                    }
                    catch (_: SocketTimeoutException)
                    {
                        // do nothing
                    }
                    catch (e: Exception)
                    {
                        e.printStackTrace()
                    }
                }
                try
                {
                    decoder?.stop()
                    decoder?.release()
                    receiverSocket.close()
                }
                catch (e: Exception)
                {
                    e.printStackTrace()
                }
            }.start()
        }
        catch (ee: Exception)
        {
            ee.printStackTrace()
        }
    }

    private fun yuv420ToBitmap(yuvBytes: ByteArray, width: Int, height: Int): Bitmap
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

    fun stopReceive()
    {
        isReceiving = false
    }

    fun setBitmapReceiver(target: IBitmapReceiver)
    {
        bitmapReceiver = target
    }

    companion object
    {
        private val TAG = StreamReceiver::class.java.simpleName
        private const val TIMEOUT_US = 10000L
        private const val BUFFER_SIZE = 256 * 1024 + 16 // 受信バッファは 256kB
        private const val TIMEOUT_MS = 5500
        private const val VIDEO_WIDTH = 960
        private const val VIDEO_HEIGHT = 720
        private const val MIME_TYPE = "video/avc"
    }
}
*/

class StreamReceiver(private val streamPortNo: Int = STREAM_PORT, private val width: Int = VIDEO_WIDTH, private val height: Int = VIDEO_HEIGHT)
{
    companion object
    {
        private val TAG = StreamReceiver::class.java.simpleName
        private const val BUFFER_SIZE = 256 * 1024 + 16  // 受信バッファは 256kB
        private const val TIMEOUT_MS = 100L
        private const val STREAM_PORT = 11111
        private const val VIDEO_WIDTH = 960
        private const val VIDEO_HEIGHT = 720
    }

    private val bufferSize = width * height * 3 / 2 // YUV420形式を想定
    private val receiveQueue = ArrayBlockingQueue<ByteArray>(100)
    private val streamBuffer = ByteArrayOutputStream()
    private var running = false
    private lateinit var decoder: MediaCodec
    private lateinit var bitmapReceiver: IBitmapReceiver

    fun startReceive()
    {
        running = true
        Thread { receiveUdpDataThread(streamPortNo) }.start()
        Thread { decodeDataThread() }.start()
    }

    fun stopReceive()
    {
        try
        {
            Log.v(TAG, "stopReceive()")
            running = false
            decoder.stop()
            decoder.release()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }


    private fun receiveUdpDataThread(port: Int)
    {
        // ----------  UDPのデータ受信スレッド
        val socket = DatagramSocket(port)
        try
        {
            var receiveCount = 0
            val buffer = ByteArray(BUFFER_SIZE) // メッセージ受信バッファ
            val packet = DatagramPacket(buffer, buffer.size)
            while (running)
            {
                socket.receive(packet)
                val receivedData = packet.data.copyOf(packet.length)
                streamBuffer.write(receivedData, 0, packet.length)
                if ((packet.length < 1000)||(receiveCount > 20))
                {
                    receiveQueue.offer(streamBuffer.toByteArray(), TIMEOUT_MS, TimeUnit.MILLISECONDS) // キューに追加
                    streamBuffer.reset()
                    receiveCount = 0
                }
                receiveCount++
                //Log.v(TAG, "RECEIVED DATA : ${receivedData.size}")
                //receiveQueue.offer(receivedData, TIMEOUT_MS, TimeUnit.MILLISECONDS) // キューに追加
            }
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error receiving data: ${e.message}")
        }
        finally
        {
            Log.v(TAG, " FINISH receiveUdpDataThread(port: $port)")
            socket.close()
        }
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

    private fun decodeDataThread()
    {
        Log.v(TAG, "Start decodeData()")
        try
        {
            initializeDecoder()
            while (running)
            {
                try
                {
                    Thread.sleep(TIMEOUT_MS)  // 取得前、少し待つ
                    val receivedUdpData = receiveQueue.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    receivedUdpData?.let {
                        if (::decoder.isInitialized)
                        {
                            Log.v(TAG, "PICKED DATA (SIZE: ${it.size} )")
                            try
                            {
                                val inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_MS)
                                if (inputBufferIndex >= 0)
                                {
                                    val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
                                    inputBuffer?.clear()
                                    inputBuffer?.put(it)
                                    decoder.queueInputBuffer(inputBufferIndex, 0, it.size, 0, 0)
                                }
                            }
                            catch (ex: java.lang.IllegalStateException)
                            {
                                Log.v(TAG, "java.lang.IllegalStateException......")
                            }
                            catch (xex: Exception)
                            {
                                // 例外を食べる
                                xex.printStackTrace()
                            }

                            val bufferInfo = MediaCodec.BufferInfo()
                            var outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 100)
                            while (outputBufferIndex >= 0)
                            {
                                // ここでデコードされたデータ（通常はYUV形式）を処理
                                try
                                {
                                    val yuvBytes = ByteArray(bufferSize)
                                    val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)
                                    outputBuffer?.position(bufferInfo.offset)
                                    outputBuffer?.limit(bufferInfo.offset + bufferInfo.size)
                                    outputBuffer?.get(yuvBytes)
                                    val bitmap = yuv420ToBitmap(yuvBytes, width, height)
                                    if (::bitmapReceiver.isInitialized)
                                    {
                                        Log.v(TAG, "UPDATE BITMAP (size: ${bitmap.width} x ${bitmap.height})")
                                        bitmapReceiver.updateBitmapImage(bitmap)
                                    }
                                }
                                catch (ee: Exception)
                                {
                                    ee.printStackTrace()
                                }
                                decoder.releaseOutputBuffer(outputBufferIndex, false)
                                outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0)
                            }
                        }
                    }
                }
                catch (ex: java.lang.IllegalStateException)
                {
                    Log.v(TAG, "java.lang.IllegalStateException...")
                }
                catch (ex: Exception)
                {
                    ex.printStackTrace()
                }
            }
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error decoding data: ${e.message}")
        }
        finally
        {
            Log.v(TAG, "Decoder Finished.")
            try
            {
                decoder.stop()
                decoder.release()
            }
            catch (ee: Exception)
            {
                ee.printStackTrace()
            }
        }
    }

    fun setBitmapReceiver(target: IBitmapReceiver)
    {
        bitmapReceiver = target
    }

    // YUVデータをBitmapに変換する関数（実装は非常に複雑）
    private fun yuv420ToBitmap(yuvBytes: ByteArray, width: Int, height: Int): Bitmap
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
}
