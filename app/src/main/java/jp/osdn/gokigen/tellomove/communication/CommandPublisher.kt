package jp.osdn.gokigen.tellomove.communication

import android.util.Log
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
                Thread.sleep(COMMAND_POLL_QUEUE_MS.toLong())
                Log.v(TAG, " --- RECEIVE FOR REPLY --- ")
                receiveFromTello(command)
            }
            Thread.sleep(COMMAND_POLL_QUEUE_MS.toLong())
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    private fun issueCommand(command: TelloCommand)
    {
        try
        {

        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    private fun receiveFromTello(command: TelloCommand)
    {
        try
        {

        }
        catch (e: Exception)
        {
            e.printStackTrace()
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
        private const val BUFFER_SIZE = 1024 * 1024 + 16 // 受信バッファは 1MB
        private const val COMMAND_POLL_QUEUE_MS = 35
    }
}