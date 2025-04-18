package jp.osdn.gokigen.tellomove

import android.app.Application
import android.util.Log
import jp.osdn.gokigen.tellomove.communication.CommandPublisher
import jp.osdn.gokigen.tellomove.communication.StatusReceiver
import jp.osdn.gokigen.tellomove.communication.StatusWatchDog
import jp.osdn.gokigen.tellomove.communication.StreamReceiver

class AppSingleton : Application()
{
    private var isInitialized = false
    override fun onCreate()
    {
        super.onCreate()
        try
        {
            Log.v(TAG, "AppSingleton::create()")
            publisher = CommandPublisher()
            receiver = StatusReceiver()
            receiver2nd = StatusReceiver(statusPortNo = 8899, isDump = false)
            streamReceiver = StreamReceiver()
            watchdog = StatusWatchDog()
            starter = ProcessStarter(this)
            startProcess()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    override fun onTerminate()
    {
        super.onTerminate()
        try
        {
            isInitialized = false
            publisher.stop()
            receiver.stopReceive()
            receiver2nd.stopReceive()
            streamReceiver.stopReceive()
            watchdog.stopWatchDog()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    fun startProcess()
    {
        try
        {
            if (!isInitialized)
            {
                receiver.startReceive()
                receiver2nd.startReceive()
                streamReceiver.startReceive()
                publisher.start()
                watchdog.startWatchDog()
                isInitialized = true
                Thread.sleep(WAIT_MS)
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    class ProcessStarter(private val parent: AppSingleton)
    {
        fun start()
        {
            parent.startProcess()
        }
    }

    companion object
    {
        lateinit var publisher: CommandPublisher
        lateinit var receiver: StatusReceiver
        lateinit var receiver2nd: StatusReceiver
        lateinit var streamReceiver: StreamReceiver
        lateinit var watchdog: StatusWatchDog
        lateinit var starter: ProcessStarter
        private const val WAIT_MS = 25L
        private val TAG = AppSingleton::class.java.simpleName
    }
}
