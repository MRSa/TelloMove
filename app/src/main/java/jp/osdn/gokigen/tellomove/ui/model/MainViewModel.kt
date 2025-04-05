package jp.osdn.gokigen.tellomove.ui.model
import android.content.ContentResolver
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import jp.osdn.gokigen.tellomove.AppSingleton
import jp.osdn.gokigen.tellomove.communication.IConnectionStatusUpdate

class MainViewModel: ViewModel(), IConnectionStatusUpdate
{
    private lateinit var contentResolver: ContentResolver

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

    fun initializeViewModel(activity: AppCompatActivity)
    {
        try
        {
            Log.v(TAG, "MainViewModel::initializeViewModel()")
            contentResolver = activity.contentResolver
            statusMessageString.value = ""
            isConnected.value = false
            moveDistance.value = 50
            turnDegree.value = 90
            currentSpeed.value = 20
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
        isConnected.value = isConnect
        AppSingleton.publisher.setConnectionStatus(isConnect)
    }

    fun setStatusMessage(status: String)
    {
        try
        {
            statusMessageString.value = status
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
