package jp.osdn.gokigen.tellomove.preference

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.core.content.edit

class PreferenceValueInitializer
{
    fun initializePreferences(context : Context)
    {
        try
        {
            Log.v(TAG, "initializePreferences()")
            val preferences = PreferenceManager.getDefaultSharedPreferences(context) ?: return
            val items : Map<String, *> = preferences.all
            preferences.edit() {
                if (!items.containsKey(IPreferencePropertyAccessor.PREFERENCE_USE_WATCHDOG)) {
                    putBoolean(
                        IPreferencePropertyAccessor.PREFERENCE_USE_WATCHDOG,
                        IPreferencePropertyAccessor.PREFERENCE_USE_WATCHDOG_DEFAULT_VALUE
                    )
                }
                if (!items.containsKey(IPreferencePropertyAccessor.PREFERENCE_SPEAK_COMMANDS)) {
                    putBoolean(
                        IPreferencePropertyAccessor.PREFERENCE_SPEAK_COMMANDS,
                        IPreferencePropertyAccessor.PREFERENCE_SPEAK_COMMANDS_DEFAULT_VALUE
                    )
                }
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    companion object
    {
        private val TAG = PreferenceValueInitializer::class.java.simpleName
    }
}