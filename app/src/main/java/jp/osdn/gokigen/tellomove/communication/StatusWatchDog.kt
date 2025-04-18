package jp.osdn.gokigen.tellomove.communication

import android.util.Log
import jp.osdn.gokigen.tellomove.AppSingleton

class StatusWatchDog
{
    private var isStarted = false
    private var useWatchdog = true
    private lateinit var reportBatteryStatus: IStatusUpdate

    fun startWatchDog()
    {
        try
        {
            Log.v(TAG, "startWatchDog() : $useWatchdog")
            isStarted = true
            Thread {
                while ((isStarted)&&(useWatchdog))
                {
                    try
                    {
                        if (AppSingleton.publisher.isConnected()) {
                            AppSingleton.publisher.enqueueCommand("battery?", object : ICommandResult {

                                override fun queuedCommand(command: String) {
                                    Log.v(TAG, "QUEUED($command)")
                                }

                                override fun commandResult(command: String, receivedStatus: Boolean, detail: String) {
                                    Log.v(TAG, "RECEIVE($command) : $receivedStatus, $detail")
                                    try
                                    {
                                        if ((::reportBatteryStatus.isInitialized)&&(detail.isNotEmpty()))
                                        {
                                            reportBatteryStatus.updateBatteryRemain(detail.toInt())
                                        }
                                    }
                                    catch (e: Exception)
                                    {
                                        e.printStackTrace()
                                    }
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

    fun setUseWatchdog(isUse: Boolean) { useWatchdog = isUse}

    fun setReportBatteryStatus(target: IStatusUpdate)
    {
        reportBatteryStatus = target
    }

    companion object
    {
        private val TAG = StatusWatchDog::class.java.simpleName
        private const val WAIT_LIMIT = 12L * 1000L  // 12000ms = 12sec.
    }
}
