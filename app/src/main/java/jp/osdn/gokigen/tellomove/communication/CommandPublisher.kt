package jp.osdn.gokigen.tellomove.communication

import android.util.Log
import java.util.ArrayDeque
import java.util.Queue

class CommandPublisher(private val ipAddress: String = "192.168.10.1", private val commandPortNo: Int = 8889) : ICommandPublisher
{
    private val commandQueue : Queue<TelloCommand> = ArrayDeque()

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

    override fun isConnected(): Boolean
    {
        TODO("Not yet implemented")
    }

    override fun start()
    {
        try
        {

        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    override fun stop()
    {
        try
        {

        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    companion object
    {
        private val TAG = CommandPublisher::class.java.simpleName
        private const val BUFFER_SIZE = 1024 * 1024 + 16 // 受信バッファは 1MB
        private const val COMMAND_SEND_RECEIVE_DURATION_MS = 30
        private const val COMMAND_SEND_RECEIVE_DURATION_MAX = 3000
        private const val COMMAND_POLL_QUEUE_MS = 15
    }

}