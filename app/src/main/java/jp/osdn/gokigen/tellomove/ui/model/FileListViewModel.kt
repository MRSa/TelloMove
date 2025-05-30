package jp.osdn.gokigen.tellomove.ui.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import jp.osdn.gokigen.tellomove.R
import jp.osdn.gokigen.tellomove.communication.IBitmapReceiver
import jp.osdn.gokigen.tellomove.file.ExportMovieFile
import jp.osdn.gokigen.tellomove.file.IFileOperationNotify
import jp.osdn.gokigen.tellomove.file.LocalFileOperation
import jp.osdn.gokigen.tellomove.file.NALToMP4Converter2
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

    private val _exportImageBitmap : MutableLiveData<Bitmap> by lazy { MutableLiveData<Bitmap>() }
    val exportImageBitmap: LiveData<Bitmap> = _exportImageBitmap

    fun initializeViewModel(activity: AppCompatActivity)
    {
        CoroutineScope(Dispatchers.Main).launch {
            try
            {
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

    fun exportAsMP4File(context: Context, fileName: String, callback: IFileOperationNotify)
    {
        CoroutineScope(Dispatchers.Main).launch {
            try
            {
                if (::fileOperation.isInitialized) {
                    val bitmap = ContextCompat.getDrawable(context, R.drawable.tello1)?.toBitmap()
                    if (bitmap != null)
                    {
                        _exportImageBitmap.value = bitmap
                    }
                    _executing.value = true

                    val outputFileName = fileName.replace(".mov","")
                    val converter = NALToMP4Converter2(context)
                    converter.convertNALToMp4(inputFileName = fileName, outputFileName = outputFileName, bitmapNotify = object : IBitmapReceiver {
                        override fun updateBitmapImage(bitmap: Bitmap) {
                            CoroutineScope(Dispatchers.Main).launch {
                                try
                                {
                                    _exportImageBitmap.value = bitmap
                                }
                                catch (t: Throwable)
                                {
                                    t.printStackTrace()
                                }
                            }
                        }
                    })

                    callback.onCompletedExport(true, fileName)
                    _executing.value = false
                    _exportImageBitmap.value = null
                    _selectedFileName.value = ""
                }
            }
            catch (t: Throwable)
            {
                t.printStackTrace()
                _executing.value = false
                callback.onCompletedExport(false, fileName)
            }
        }
    }

    fun exportMovieFile(context: Context, fileName: String, callback: IFileOperationNotify)
    {
        CoroutineScope(Dispatchers.Main).launch {
            try
            {
                    if (::fileOperation.isInitialized) {
                        _executing.value = true

                        //val outputFileName = fileName.replace(".mov","")
                        //val converter = NALToMP4Converter2(context)
                        //converter.convertNALToMp4(fileName, outputFileName)

                        val exporter = ExportMovieFile(context)
                        exporter.exportBinaryFileExternal(fileName, fileName)

                        callback.onCompletedExport(true, fileName)
                        _executing.value = false
                        _selectedFileName.value = ""
                    }
            }
            catch (t: Throwable)
            {
                t.printStackTrace()
                _executing.value = false
                callback.onCompletedExport(false, fileName)
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
