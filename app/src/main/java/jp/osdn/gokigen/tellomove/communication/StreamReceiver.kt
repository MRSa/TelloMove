package jp.osdn.gokigen.tellomove.communication

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import androidx.core.graphics.createBitmap

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
