package jp.osdn.gokigen.tellomove.communication

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.charset.Charset
import java.util.ArrayDeque
import java.util.Queue

class CommandPublisher(private val ipAddress: String = "192.168.10.1", private val commandPortNo: Int = 8889) : ICommandPublisher
{
    private val commandQueue : Queue<TelloCommand> = ArrayDeque()
    private var isConnected = false
    private var isStarted = false

    init
    {
        Log.v(TAG, "CommandPublisher : start")
        commandQueue.clear()
    }

    override fun enqueueCommand(command: String, callback: ICommandResult?): Boolean
    {
        try
        {
            Log.v(TAG, "Enqueue : $command")
            return commandQueue.offer(TelloCommand(command, callback))
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
        return (false)
    }

    override fun isConnected(): Boolean { return (isConnected) }

    fun start()
    {
        try
        {
            Log.v(TAG, "CommandPublisher::start()")
            Thread {
                try
                {
                    isStarted = true
                    while (isStarted)
                    {
                        commandSender()
                    }
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

    private fun commandSender()
    {
        try
        {
            val command = commandQueue.poll()
            if (command != null)
            {
                issueCommand(command)
            }
            Thread.sleep(COMMAND_POLL_QUEUE_MS.toLong())
        }
        catch (ex: Exception)
        {
            ex.printStackTrace()
        }
    }

    private fun issueCommand(command: TelloCommand)
    {
        try
        {
            var result: Boolean
            var replyDetail = ""
            DatagramSocket().use { socket ->
                val serverAddress = InetAddress.getByName(ipAddress)
                val sendData = command.command.toByteArray(Charset.forName("UTF-8"))
                val sendPacket = DatagramPacket(sendData, sendData.size, serverAddress, commandPortNo)

                // データ送信
                socket.send(sendPacket)

                // 受信バッファを用意
                val receiveBuffer = ByteArray(BUFFER_SIZE)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

                // 受信タイムアウトを設定
                socket.soTimeout = COMMAND_SEND_TIMEOUT_MS

                try
                {
                    socket.receive(receivePacket)

                    Log.v(TAG, " <<<<< RECEIVED REPLY ${command.command} $replyDetail ")
                    replyDetail = String(receivePacket.data, 0, receivePacket.length, Charset.forName("UTF-8"))
                    result = true
                }
                catch (e: SocketTimeoutException)
                {
                    Log.v(TAG, "<<<<< TIMEOUT ${command.command}")
                    result = false
                    replyDetail = ""
                }
                catch (e: Exception)
                {
                    Log.v(TAG, "<<<<< EXCEPTION ${command.command}")
                    result = false
                    replyDetail = ""
                }
            }
            command.callback?.commandResult(command.command, result, replyDetail)
        }
        catch (ee: Exception)
        {
            ee.printStackTrace()
        }
    }

    fun stop()
    {
        try
        {
            Log.v(TAG, "CommandPublisher::stop()")
            isStarted = false
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    fun setConnectionStatus(status: Boolean)
    {
        isConnected = status
    }

    companion object
    {
        private val TAG = CommandPublisher::class.java.simpleName
        private const val BUFFER_SIZE = 384 * 1024 + 16  // 受信バッファは 384kB
        private const val COMMAND_POLL_QUEUE_MS = 35
        private const val COMMAND_SEND_TIMEOUT_MS = 3500  // 3500ms
    }
}
