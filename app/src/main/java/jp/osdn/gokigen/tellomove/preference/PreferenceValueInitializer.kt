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
                if (!items.containsKey(IPreferencePropertyAccessor.PREFERENCE_CHECK_PRODUCT_ID)) {
                    putBoolean(
                        IPreferencePropertyAccessor.PREFERENCE_CHECK_PRODUCT_ID,
                        IPreferencePropertyAccessor.PREFERENCE_CHECK_PRODUCT_ID_DEFAULT_VALUE
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