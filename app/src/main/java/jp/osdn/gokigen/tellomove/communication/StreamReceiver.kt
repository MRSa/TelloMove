package jp.osdn.gokigen.tellomove.communication

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import java.nio.charset.Charset

class StreamReceiver(private val streamPortNo: Int = 11111, private val isDump: Boolean = false)
{
    private var isReceiving = false
    private lateinit var receiverSocket: DatagramSocket
    private lateinit var bitmapReceiver: IBitmapReceiver

    fun startReceive()
    {
        val buffer = ByteArray(BUFFER_SIZE)
        isReceiving = true
        try
        {
            Thread {
                receiverSocket = DatagramSocket(streamPortNo)
                while (isReceiving)
                {
                    try
                    {
                        val receivePacket = DatagramPacket(buffer, buffer.size)
                        if (::receiverSocket.isInitialized)
                        {
                            receiverSocket.soTimeout = TIMEOUT_MS
                            receiverSocket.receive(receivePacket)
                            checkReceiveData(receivePacket)
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
            }.start()
        }
        catch (ee: Exception)
        {
            ee.printStackTrace()
        }
    }

    private fun checkReceiveData(packet: DatagramPacket)
    {
        try
        {
            val receivedStatus = String(packet.data, 0, packet.length, Charset.forName("UTF-8"))
            if (isDump)
            {
                Log.v(TAG, " Status (port:${streamPortNo}, length:${receivedStatus.length}) $receivedStatus ")
            }
            if (::bitmapReceiver.isInitialized)
            {
                Log.v(TAG, "UPDATE BITMAP")
                //statusUpdateReport.updateStatus(receivedStatus)
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
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
        private const val BUFFER_SIZE = 256 * 1024 + 16 // 受信バッファは 256kB
        private const val TIMEOUT_MS = 5500
    }
}
