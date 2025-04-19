package jp.osdn.gokigen.tellomove.ui.model

import android.graphics.Bitmap
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import jp.osdn.gokigen.tellomove.AppSingleton
import jp.osdn.gokigen.tellomove.communication.IBitmapReceiver
import jp.osdn.gokigen.tellomove.communication.IStatusUpdate
import jp.osdn.gokigen.tellomove.communication.IConnectionStatusUpdate
import jp.osdn.gokigen.tellomove.preference.IPreferencePropertyAccessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import jp.osdn.gokigen.tellomove.R

class MainViewModel: ViewModel(), IConnectionStatusUpdate, IStatusUpdate, IBitmapReceiver
{
    private val isConnected : MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val isTelloConnected: LiveData<Boolean> = isConnected

    private val isVideoStream : MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val isVideoStreamOn: LiveData<Boolean> = isVideoStream

    private val informationMessageString : MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val informationMessage : LiveData<String> = informationMessageString

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

    private val imageStreamBitmap : MutableLiveData<Bitmap> by lazy { MutableLiveData<Bitmap>() }
    val imageBitmap: LiveData<Bitmap> = imageStreamBitmap

    fun initializeViewModel(activity: AppCompatActivity)
    {
        try
        {
            Log.v(TAG, "MainViewModel::initializeViewModel()")
            informationMessageString.value = ""
            statusMessageString.value = ""
            isVideoStream.value = false
            isConnected.value = false
            moveDistance.value = 50
            turnDegree.value = 90
            currentSpeed.value = 20
            batteryRemain.value = -1
            val bitmap = ContextCompat.getDrawable(activity, R.drawable.tello)?.toBitmap()
            if (bitmap != null)
            {
                imageStreamBitmap.value = bitmap
            }

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
            AppSingleton.streamReceiver.setBitmapReceiver(this)
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

    private fun parseStatusInformation(status: String)
    {
        try
        {
            if (isDump)
            {
                Log.v(TAG, "STATUS: $status")
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    override fun queuedConnectionCommand(command: String)
    {
        try
        {
            CoroutineScope(Dispatchers.Main).launch {
                isConnected.value = false
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
            parseStatusInformation(status)
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

    override fun updateCommandStatus(command: String, isSuccess: Boolean, detail: String)
    {
        try
        {
            val message = "$command : $detail "
            Log.v(TAG, message)
            CoroutineScope(Dispatchers.Main).launch {
                isConnected.value = true
                informationMessageString.value = message
                if (isSuccess)
                {
                    when (command)
                    {
                        "streamon" -> { isVideoStream.value = true }
                        "streamoff" -> { isVideoStream.value = false }
                    }
                }
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    override fun queuedCommand(command: String)
    {
        try
        {
            CoroutineScope(Dispatchers.Main).launch {
                isConnected.value = false
                informationMessageString.value = "$command ..."
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
            if (isDump)
            {
                Log.v(TAG, "BATTERY $percentage %")
            }
            CoroutineScope(Dispatchers.Main).launch {
                batteryRemain.value = percentage
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    override fun updateBitmapImage(bitmap: Bitmap)
    {
        //Log.v(TAG, "Received bitmap (size: ${bitmap.width}x${bitmap.height}) : ${calculateBitmapHash(bitmap)}")
        CoroutineScope(Dispatchers.Main).launch {
            try
            {
                imageStreamBitmap.value = bitmap
            }
            catch (t: Throwable)
            {
                t.printStackTrace()
            }
        }
    }

    companion object
    {
        private val TAG = MainViewModel::class.java.simpleName
        private const val isDump = false
    }
}
