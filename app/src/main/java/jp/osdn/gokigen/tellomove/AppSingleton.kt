package jp.osdn.gokigen.tellomove

import android.app.Application
import android.util.Log
import jp.osdn.gokigen.tellomove.communication.CommandPublisher
import jp.osdn.gokigen.tellomove.communication.StatusReceiver

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
            receiver.startReceive()
            publisher.start()
            isInitialized = true
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    fun isInitialized() : Boolean { return (isInitialized) }

    companion object
    {
        lateinit var publisher: CommandPublisher
        lateinit var receiver: StatusReceiver
        private val TAG = AppSingleton::class.java.simpleName
    }
}
