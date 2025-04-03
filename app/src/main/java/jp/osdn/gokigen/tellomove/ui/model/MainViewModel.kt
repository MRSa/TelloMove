package jp.osdn.gokigen.tellomove.ui.model
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel()
{
    private lateinit var contentResolver: ContentResolver

    private val targetUri : MutableLiveData<Uri> by lazy { MutableLiveData<Uri>() }
    val targetFileUri: LiveData<Uri> = targetUri

    private val isRunning : MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val isTelloRunning: LiveData<Boolean> = isRunning

    fun initializeViewModel(activity: AppCompatActivity)
    {
        try
        {
            Log.v(TAG, "DataImportViewModel::initializeViewModel()")
            contentResolver = activity.contentResolver
            isRunning.value = false
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
