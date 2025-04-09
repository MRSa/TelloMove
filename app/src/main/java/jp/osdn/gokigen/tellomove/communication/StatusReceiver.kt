package jp.osdn.gokigen.tellomove.communication

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import java.nio.charset.Charset

class StatusReceiver(private val statusPortNo: Int = 8890, private val isDump: Boolean = false)
{
    private var isReceiving = false
    private lateinit var receiverSocket: DatagramSocket
    private lateinit var statusUpdateReport: IStatusUpdate

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
            val receivedStatus = String(packet.data, 0, packet.length, Charset.forName("UTF-8"))
            if (isDump)
            {
                Log.v(TAG, " Status (port:${statusPortNo}, length:${receivedStatus.length}) $receivedStatus ")
            }
            if (::statusUpdateReport.isInitialized)
            {
                statusUpdateReport.updateStatus(receivedStatus)
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

    fun setStatusUpdateReport(target: IStatusUpdate)
    {
        statusUpdateReport = target
    }

    companion object
    {
        private val TAG = StatusReceiver::class.java.simpleName
        private const val BUFFER_SIZE = 256 * 1024 + 16 // 受信バッファは 256kB
        private const val TIMEOUT_MS = 5500
    }
}
