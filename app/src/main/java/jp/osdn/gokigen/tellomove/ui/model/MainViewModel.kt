package jp.osdn.gokigen.tellomove.ui.model

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import jp.osdn.gokigen.tellomove.AppSingleton
import jp.osdn.gokigen.tellomove.communication.IStatusUpdate
import jp.osdn.gokigen.tellomove.communication.IConnectionStatusUpdate
import jp.osdn.gokigen.tellomove.preference.IPreferencePropertyAccessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel: ViewModel(), IConnectionStatusUpdate, IStatusUpdate
{
    private val isConnected : MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val isTelloConnected: LiveData<Boolean> = isConnected

    private val statusMessageString : MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val statusMessage : LiveData<String> = statusMessageString

    private val moveDistance : MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val moveDistanceCm: LiveData<Int> = moveDistance

    private val turnDegree : MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val moveDegree: LiveData<Int> = turnDegree

    private val currentSpeed : MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val moveSpeed: LiveData<Int> = currentSpeed

    private val batteryRemain : MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val batteryPercent: LiveData<Int> = batteryRemain

    private val lastCommand : MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val lastCommandStatus: LiveData<Boolean> = lastCommand

    fun initializeViewModel(activity: AppCompatActivity)
    {
        try
        {
            Log.v(TAG, "MainViewModel::initializeViewModel()")
            statusMessageString.value = ""
            isConnected.value = false
            moveDistance.value = 50
            turnDegree.value = 90
            currentSpeed.value = 20
            batteryRemain.value = -1
            lastCommand.value = false

            // set preference to
            val preference = PreferenceManager.getDefaultSharedPreferences(activity)
            val useWatchdog = preference.getBoolean(
                IPreferencePropertyAccessor.PREFERENCE_USE_WATCHDOG,
                IPreferencePropertyAccessor.PREFERENCE_USE_WATCHDOG_DEFAULT_VALUE
            )
            AppSingleton.watchdog.setUseWatchdog(useWatchdog)

            // subscribe events
            AppSingleton.watchdog.setReportBatteryStatus(this)
            AppSingleton.receiver.setStatusUpdateReport(this)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    fun setDistance(distance: Int)
    {
        try
        {
            CoroutineScope(Dispatchers.Main).launch {
                moveDistance.value = distance
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    fun setDegree(degree: Int)
    {
        try
        {
            CoroutineScope(Dispatchers.Main).launch {
                turnDegree.value = degree
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    fun setSpeed(speed: Int)
    {
        try
        {
            CoroutineScope(Dispatchers.Main).launch {
                currentSpeed.value = speed
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    override fun setConnectionStatus(isConnect: Boolean)
    {
        try
        {
            CoroutineScope(Dispatchers.Main).launch {
                isConnected.value = isConnect
                AppSingleton.publisher.setConnectionStatus(isConnect)
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    override fun updateStatus(status: String)
    {
        try
        {
            if (status.isNotEmpty())
            {
                CoroutineScope(Dispatchers.Main).launch {
                    isConnected.value = true
                    statusMessageString.value = status
                }
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    override fun updateCommandStatus(command: String, isSuccess: Boolean)
    {
        try
        {
            Log.v(TAG, "$command : $isSuccess")
            CoroutineScope(Dispatchers.Main).launch {
                lastCommand.value = isSuccess
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    override fun updateBatteryRemain(percentage: Int)
    {
        try
        {
            Log.v(TAG, "BATTERY $percentage %")
            CoroutineScope(Dispatchers.Main).launch {
                batteryRemain.value = percentage
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    companion object
    {
        private val TAG = MainViewModel::class.java.simpleName
    }
}
