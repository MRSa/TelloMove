package jp.osdn.gokigen.tellomove

import android.app.Application
import android.util.Log


class AppSingleton : Application()
{
    override fun onCreate()
    {
        super.onCreate()
        try
        {
            Log.v(TAG, "AppSingleton::create()")
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    companion object
    {
        private val TAG = AppSingleton::class.java.simpleName
    }
}
