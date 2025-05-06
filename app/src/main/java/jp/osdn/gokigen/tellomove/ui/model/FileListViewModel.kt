package jp.osdn.gokigen.tellomove.ui.model

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import jp.osdn.gokigen.tellomove.file.LocalFileOperation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FileListViewModel: ViewModel()
{
    private lateinit var fileOperation: LocalFileOperation

    private val _fileNameList : MutableLiveData<List<String>> by lazy { MutableLiveData<List<String>>() }
    val fileNameList: LiveData<List<String>> = _fileNameList

    private val _selectedFileName : MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val selectedFileName: LiveData<String> = _selectedFileName

    private val _executing : MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val processExecuting: LiveData<Boolean> = _executing

    fun initializeViewModel(activity: AppCompatActivity)
    {
        CoroutineScope(Dispatchers.Main).launch {
            try
            {
                //_updatedFileList.value = false
                fileOperation = LocalFileOperation(activity)
                _fileNameList.value = fileOperation.getFileList()
                _executing.value = false
                _selectedFileName.value = ""
            }
            catch (t: Throwable)
            {
                t.printStackTrace()
            }
        }
    }

    fun updateFileNameList()
    {
        CoroutineScope(Dispatchers.Main).launch {
            try
            {
                _executing.value = true
                _fileNameList.value = fileOperation.getFileList()
                _executing.value = false
                Log.v(TAG, "updateFileNameList: ${_fileNameList.value?.size}")
            }
            catch (t: Throwable)
            {
                t.printStackTrace()
            }
        }
    }

    fun selectedFileName(fileName: String)
    {
        Log.v(TAG, "selectedFileName: $fileName")
        _selectedFileName.value = fileName
    }

    fun deleteFileName(fileName: String)
    {
        CoroutineScope(Dispatchers.Main).launch {
            try
            {
                Log.v(TAG, "deleteFileName: $fileName")
                if (fileName.isNotEmpty()) {
                    if (::fileOperation.isInitialized) {
                        _executing.value = true
                        fileOperation.deleteFile(fileName)
                        _executing.value = false
                        _selectedFileName.value = ""
                    }
                }
            }
            catch (e: Exception)
            {
                e.printStackTrace()
            }
        }
    }

    fun exportMovieFile(fileName: String)
    {
        CoroutineScope(Dispatchers.Main).launch {
            try
            {
                if (::fileOperation.isInitialized)
                {
                    _executing.value = true
                    fileOperation.exportMovieFile(fileName)
                    _executing.value = false
                    _selectedFileName.value = ""
                }
            }
            catch (t: Throwable)
            {
                t.printStackTrace()
            }
        }
    }

    fun deleteAllFiles()
    {
        CoroutineScope(Dispatchers.Main).launch {
            try
            {
                if (::fileOperation.isInitialized)
                {
                    _executing.value = true
                    fileOperation.cleanupAllFiles()
                    _executing.value = false
                    _selectedFileName.value = ""
                }
            }
            catch (t: Throwable)
            {
                t.printStackTrace()
            }
        }
    }

    companion object
    {
        private val TAG = FileListViewModel::class.java.simpleName
    }
}
