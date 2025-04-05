package jp.osdn.gokigen.tellomove.communication

import android.util.Log
import jp.osdn.gokigen.tellomove.AppSingleton

class StatusWatchDog
{
    private var isStarted = false

    fun startWatchDog()
    {
        try
        {
            Log.v(TAG, "startWatchDog() ")
            isStarted = true
            Thread {
                while (isStarted)
                {
                    try
                    {
                        if (AppSingleton.publisher.isConnected()) {
                            AppSingleton.publisher.enqueueCommand("battery?", object : ICommandResult {
                                override fun commandResult(command: String, detail: String) {
                                    Log.v(TAG, "POST($command) : $detail")
                                }
                            })
                        }
                    }
                    catch (e: Exception)
                    {
                        e.printStackTrace()
                    }
                    Thread.sleep(WAIT_LIMIT)
                }
            }.start()
        }
        catch (ee: Exception)
        {
            ee.printStackTrace()
        }
    }

    fun stopWatchDog()
    {
        isStarted = false
    }

    companion object
    {
        private val TAG = StatusWatchDog::class.java.simpleName
        private const val WAIT_LIMIT = 12L * 1000L  // 12000ms = 12sec.
    }
}
