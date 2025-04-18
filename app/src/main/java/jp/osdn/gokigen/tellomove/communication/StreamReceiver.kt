package jp.osdn.gokigen.tellomove.communication

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import androidx.core.graphics.createBitmap
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class StreamReceiver(private val streamPortNo: Int = STREAM_PORT, private val width: Int = VIDEO_WIDTH, private val height: Int = VIDEO_HEIGHT)
{
    companion object
    {
        private val TAG = StreamReceiver::class.java.simpleName
        private const val BUFFER_SIZE = 256 * 1024 + 16  // 受信バッファは 256kB
        private const val BUFFER_COUNT = 6
        private const val DATA_LENGTH_LIMIT = 1000
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
        val startCode = byteArrayOf(0x00, 0x00, 0x01) // 一般的な開始コード
        val longStartCode = byteArrayOf(0x00, 0x00, 0x00, 0x01) // 長い開始コード
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

/*
                val index = findNalUnits(receivedData, startCode, longStartCode)
                if (index < 0)
                {
                    streamBuffer.write(receivedData, 0, packet.length)
                }
                else if (index == 0)
                {
                    val queuedData = streamBuffer.toByteArray()
                    Log.v(TAG, " ----- Push data : ${queuedData.size} bytes.")
                    receiveQueue.offer(queuedData, TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    streamBuffer.reset()
                }
                else
                {
                    streamBuffer.write(receivedData, 0, index - 1)
                    val queuedData = streamBuffer.toByteArray()
                    Log.v(TAG, " ===== Push data : ${queuedData.size} bytes.")
                    receiveQueue.offer(queuedData, TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    streamBuffer.reset()
                    streamBuffer.write(queuedData, index, (queuedData.size - index))
                }
*/
                streamBuffer.write(receivedData, 0, packet.length)
                //if ((packet.length < DATA_LENGTH_LIMIT)||(receiveCount > BUFFER_COUNT))
                if (receiveCount > BUFFER_COUNT)
                {
                    val queuedData = streamBuffer.toByteArray()
                    Log.v(TAG, " ----- Push data : ${queuedData.size} bytes. (receivedCount: $receiveCount)")
                    receiveQueue.offer(queuedData, TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    streamBuffer.reset()
                    streamBuffer.reset()
                    receiveCount = 0
                }
                receiveCount++
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

    private fun findNalUnits(data: ByteArray, startCode: ByteArray, longStartCode: ByteArray) : Int
    {
        var index = 0
        while (index < data.size - 2)
        {
            var nalUnitLength = -1

            // 長い開始コード (0x00 0x00 0x00 0x01) をチェック
            if (index <= data.size - 4 && data[index] == longStartCode[0] && data[index + 1] == longStartCode[1] && data[index + 2] == longStartCode[2] && data[index + 3] == longStartCode[3])
            {
                nalUnitLength = 4
            }
            // 短い開始コード (0x00 0x00 0x01) をチェック (長い開始コードが見つからなかった場合)
            else if (data[index] == startCode[0] && data[index + 1] == startCode[1] && data[index + 2] == startCode[2])
            {
                nalUnitLength = 3
            }

            if (nalUnitLength > 0)
            {
                return (index)
            }
            else
            {
                index++
            }
        }
        return (-1)
    }

    private fun findNalUnitsOptimized(data: ByteArray, startCode: ByteArray, longStartCode: ByteArray): Int {
        val longStartCodeLength = longStartCode.size
        val shortStartCodeLength = startCode.size
        val dataSize = data.size

        for (index in 0 until dataSize - 2) {
            // まず長い開始コードをチェック
            if (index <= dataSize - longStartCodeLength) {
                var matchLong = true
                for (i in 0 until longStartCodeLength) {
                    if (data[index + i] != longStartCode[i]) {
                        matchLong = false
                        break
                    }
                }
                if (matchLong) {
                    return index
                }
            }

            // 次に短い開始コードをチェック (長い開始コードのチェック範囲外の場合のみ)
            if (index <= dataSize - shortStartCodeLength) {
                var matchShort = true
                for (i in 0 until shortStartCodeLength) {
                    if (data[index + i] != startCode[i]) {
                        matchShort = false
                        break
                    }
                }
                if (matchShort) {
                    return index
                }
            }
        }
        return -1
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
        Log.v(TAG, " ----- Start decodeDataThread()")
        try
        {
            initializeDecoder()
            while (running)
            {
                try
                {
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
                                // デコードされたデータを処理
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
