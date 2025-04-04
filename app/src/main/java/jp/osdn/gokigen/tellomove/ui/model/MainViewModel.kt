package jp.osdn.gokigen.tellomove.ui.model
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel()
{
    private lateinit var contentResolver: ContentResolver

    private val isConnected : MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val isTelloConnected: LiveData<Boolean> = isConnected

    private val isRunning : MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val isTelloRunning: LiveData<Boolean> = isRunning

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
            isRunning.value = false
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

    fun setConnected(isConnect: Boolean) { isConnected.value = isConnect }
    fun setDistance(distance: Int) { moveDistance.value = distance }
    fun setDegree(degree: Int) { moveDistance.value = degree }
    fun setSpeed(speed: Int) { currentSpeed.value = speed }

    companion object
    {
        private val TAG = MainViewModel::class.java.simpleName
    }
}
