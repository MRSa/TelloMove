package jp.osdn.gokigen.tellomove.communication

import android.util.Log

class TelloCommandCallback(private val updateReceiver: IStatusUpdate) : ICommandResult
{
    override fun queuedCommand(command: String)
    {
        try
        {
            updateReceiver.queuedCommand(command)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    override fun commandResult(command: String, receivedStatus: Boolean, detail: String)
    {
        Log.v(TAG, "commandResult: $command ($receivedStatus) $detail")
        try
        {
            val isSuccess = detail.contains("ok")
            updateReceiver.updateCommandStatus(command, isSuccess, detail)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    companion object
    {
        private val TAG = TelloCommandCallback::class.java.simpleName
    }
}
