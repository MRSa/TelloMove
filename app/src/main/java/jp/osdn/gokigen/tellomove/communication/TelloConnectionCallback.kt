package jp.osdn.gokigen.tellomove.communication

import android.util.Log

class TelloConnectionCallback(private val connection: IConnectionStatusUpdate) : ICommandResult
{
    override fun queuedCommand(command: String)
    {
        try
        {
            connection.queuedConnectionCommand(command)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    override fun commandResult(command: String, receivedStatus: Boolean, detail: String)
    {
        Log.v(TAG, "commandResult(connection): $command ($receivedStatus) $detail")
        try
        {
            val isConnect = detail.contains("ok")
            connection.setConnectionStatus(isConnect)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    companion object
    {
        private val TAG = TelloConnectionCallback::class.java.simpleName
    }
}
