package jp.osdn.gokigen.tellomove.ui.model

import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.core.content.edit
import androidx.preference.PreferenceManager

import jp.osdn.gokigen.tellomove.preference.IPreferencePropertyAccessor
import jp.osdn.gokigen.tellomove.speakcommand.ISpeakCommandStatusUpdate

class PreferenceViewModel: ViewModel()
{
    private lateinit var preference : SharedPreferences
    private lateinit var speakCommandsCallback : ISpeakCommandStatusUpdate

    private val usePollingCommand : MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val useWatchdog: LiveData<Boolean> = usePollingCommand

    private val speakCommand : MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val speakCommands: LiveData<Boolean> = speakCommand

    fun initializeViewModel(activity: AppCompatActivity, speakStatusUpdate: ISpeakCommandStatusUpdate)
    {
        try
        {
            speakCommandsCallback = speakStatusUpdate
            preference = PreferenceManager.getDefaultSharedPreferences(activity)
            usePollingCommand.value = preference.getBoolean(
                IPreferencePropertyAccessor.PREFERENCE_USE_WATCHDOG,
                IPreferencePropertyAccessor.PREFERENCE_USE_WATCHDOG_DEFAULT_VALUE
            )
            speakCommand.value = preference.getBoolean(
                IPreferencePropertyAccessor.PREFERENCE_SPEAK_COMMANDS,
                IPreferencePropertyAccessor.PREFERENCE_SPEAK_COMMANDS_DEFAULT_VALUE
            )
            Log.v(TAG, "PreferenceViewModel::initializeViewModel() ")
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    fun setUseWatchDog(value: Boolean)
    {
        try
        {
            if (!::preference.isInitialized)
            {
                Log.v(TAG, " Preference Manager is unknown...")
                return
            }
            preference.edit {
                putBoolean(IPreferencePropertyAccessor.PREFERENCE_USE_WATCHDOG, value)
            }
            usePollingCommand.value = value
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    fun setSpeakCommand(value: Boolean)
    {
        try
        {
            if (!::preference.isInitialized)
            {
                Log.v(TAG, " Preference Manager is unknown...")
                return
            }
            preference.edit {
                putBoolean(IPreferencePropertyAccessor.PREFERENCE_SPEAK_COMMANDS, value)
            }
            speakCommand.value = value
            if (::speakCommandsCallback.isInitialized)
            {
                speakCommandsCallback.setSpeakCommandStatus(value)
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    companion object
    {
        private val TAG = PreferenceViewModel::class.java.simpleName
    }
}
