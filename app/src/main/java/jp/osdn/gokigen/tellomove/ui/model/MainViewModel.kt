package jp.osdn.gokigen.tellomove.ui.model

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import jp.osdn.gokigen.tellomove.AppSingleton
import jp.osdn.gokigen.tellomove.communication.IStatusUpdate
import jp.osdn.gokigen.tellomove.communication.IConnectionStatusUpdate
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

            // subscribe events
            AppSingleton.watchdog.setReportBatteryStatus(this)
            AppSingleton.receiver.setStatusUpdateReport(this)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    fun setDistance(distance: Int) { moveDistance.value = distance }
    fun setDegree(degree: Int) { moveDistance.value = degree }
    fun setSpeed(speed: Int) { currentSpeed.value = speed }

    override fun setConnectionStatus(isConnect: Boolean)
    {
        CoroutineScope(Dispatchers.Main).launch {
            isConnected.value = isConnect
            AppSingleton.publisher.setConnectionStatus(isConnect)
        }
    }

    override fun updateStatus(status: String)
    {
        try
        {
            CoroutineScope(Dispatchers.Main).launch {
                statusMessageString.value = status
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
