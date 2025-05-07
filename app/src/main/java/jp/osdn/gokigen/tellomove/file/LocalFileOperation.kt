package jp.osdn.gokigen.tellomove.file

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

class LocalFileOperation(private val context: Context)
{
    fun getFileList(topDir :String = "TelloMove") : List<String>
    {
        val fileList = ArrayList<String>()
        try
        {
            fileList.clear()
            val baseDir = context.getExternalFilesDir(null)
            val dirPath = "${baseDir?.absolutePath}/$topDir"
            val destination = File(dirPath)
            val files = destination.listFiles()
            files?.forEach { file ->
                try
                {
                    if (file.isFile)
                    {
                        fileList.add(file.name)
                    }
                }
                catch (ee: Exception)
                {
                    ee.printStackTrace()
                }
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
        return (fileList)
    }

    fun cleanupAllFiles(topDir :String = "TelloMove")
    {
        val baseDir = context.getExternalFilesDir(null)
        val dirPath = "${baseDir?.absolutePath}/$topDir"
        try
        {
            cleanupDirectory(dirPath)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    private fun cleanupDirectory(dirPath: String)
    {
        try
        {
            Log.v(TAG, "cleanupDirectory() : $dirPath")
            val destination = File(dirPath)
            if ((destination.exists())&&(destination.isDirectory))
            {
                val files = destination.listFiles()
                files?.forEach { file ->
                    try
                    {
                        if (file.isFile) {
                            val result = file.delete()
                            Log.v(TAG, "Delete File : ${file.name} ($result)")
                        } else if (file.isDirectory) {
                            // 再帰的にサブディレクトリも削除
                            cleanupDirectory(file.absolutePath)
                            val result = file.delete()
                            Log.v(TAG, "Delete Dir. : ${file.name} ($result)")
                        }
                    }
                    catch (ee: Exception)
                    {
                        ee.printStackTrace()
                    }
                }
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    fun deleteFile(fileName: String, outputDir :String = "TelloMove")
    {
        try
        {
            Log.v(TAG, "deleteFile: $outputDir/$fileName")
            val file = getFileLocal(fileName)
            if (file == null)
            {
                Log.v(TAG, "exportMovieFile($fileName) : file get failure")
                return
            }
            file.delete()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    fun exportMovieFile(fileName: String, outputDir :String = "TelloMove"): Boolean
    {
        //  ----- ビットマップデータを(JPEG形式で)保管する。
        var result = false
        try
        {
            Log.v(TAG, "exportMovieFile($fileName) to $outputDir/")
            val file = getFileLocal(fileName)
            if (file == null)
            {
                Log.v(TAG, "exportMovieFile($fileName) : file get failure")
                return (false)
            }

            //val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path + "/" + AppSingleton.APP_NAMESPACE + "/"
            val resolver = context.contentResolver
            var outputStream: OutputStream? = null
            val extStorageUri: Uri
            var movieUri: Uri? = null
            val values = ContentValues()
            values.put(MediaStore.Video.Media.TITLE, fileName)
            values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                values.put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$outputDir/")
                values.put(MediaStore.Video.Media.IS_PENDING, true)
                extStorageUri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                Log.v(TAG, "---------- $fileName $values")
                movieUri = resolver.insert(extStorageUri, values)
                if (movieUri != null)
                {
                    ////////////////////////////////////////////////////////////////
                    if (DUMPLOG)
                    {
                        try
                        {
                            val cursor = resolver.query(movieUri, null, null, null, null)
                            DatabaseUtils.dumpCursor(cursor)
                            cursor!!.close()
                        }
                        catch (e: Exception)
                        {
                            e.printStackTrace()
                        }
                    }
                    ////////////////////////////////////////////////////////////////
                    try
                    {
                        outputStream = resolver.openOutputStream(movieUri, "wa")
                    }
                    catch (ee: Exception)
                    {
                        ee.printStackTrace()
                    }
                }
                else
                {
                    Log.v(TAG, " cannot get imageUri...")
                }
            }
            else
            {
                val path = File(outputDir)
                if (!path.mkdir())
                {
                    Log.v(TAG, " mkdir fail: $outputDir")
                }
                values.put(
                    MediaStore.Video.Media.DATA, path.absolutePath + File.separator + fileName
                )
                val targetPath = File(outputDir + File.separator + fileName)
                try
                {
                    outputStream = FileOutputStream(targetPath)
                }
                catch (e: Exception)
                {
                    e.printStackTrace()
                }
            }
            if (outputStream != null)
            {
                try
                {
                    val inputStream = FileInputStream(file)
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                            Log.i(TAG, "Movie file copied to: $movieUri")
                        }
                    }
                }
                catch (ex: Exception)
                {
                    ex.printStackTrace()
                }
                outputStream.flush()
                outputStream.close()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                values.put(MediaStore.Video.Media.IS_PENDING, false)
                if (movieUri != null)
                {
                    resolver.update(movieUri, values, null, null)
                }
            }
            result = true
        }
        catch (t: Throwable)
        {
            t.printStackTrace()
        }
        return (result)
    }

    private fun getFileLocal(fileName: String, topDir :String = "TelloMove") : File?
    {
        try
        {
            val baseDir = context.getExternalFilesDir(null)
            val filePath = "${baseDir?.absolutePath}/$topDir/$fileName"
            val file = File(filePath)
            if (file.exists())
            {
                return (file)
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
        return (null)
    }

    companion object
    {
        private val  TAG = LocalFileOperation::class.java.simpleName
        private const val DUMPLOG = false
    }
}
