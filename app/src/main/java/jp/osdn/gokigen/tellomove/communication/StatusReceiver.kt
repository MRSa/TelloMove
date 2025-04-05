package jp.osdn.gokigen.tellomove.communication

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException

class StatusReceiver(private val statusPortNo: Int = 8890)
{
    private var isReceiving = false
    private lateinit var receiverSocket: DatagramSocket

    fun startReceive()
    {
        val buffer = ByteArray(BUFFER_SIZE)
        isReceiving = true
        try
        {
            Thread {
                receiverSocket = DatagramSocket(statusPortNo)
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
            val dataLength: Int = packet.length
            val receivedData: ByteArray = packet.data

            Log.v(TAG, " checkReceiveData (length:$dataLength [${receivedData.size}]) ")
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

    companion object
    {
        private val TAG = CommandPublisher::class.java.simpleName
        private const val BUFFER_SIZE = 1024 * 1024 + 16 // 受信バッファは 1MB
        private const val TIMEOUT_MS = 5500
    }
}
