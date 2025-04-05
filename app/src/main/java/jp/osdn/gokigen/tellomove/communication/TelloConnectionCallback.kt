package jp.osdn.gokigen.tellomove.communication

import android.util.Log

class TelloConnectionCallback(private val connection: IConnectionStatusUpdate) : ICommandResult
{
    override fun commandResult(command: String, detail: String)
    {
        Log.v(TAG, "commandResult: $command ($detail)")
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
