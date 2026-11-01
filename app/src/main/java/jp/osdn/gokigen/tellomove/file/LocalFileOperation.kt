package jp.osdn.gokigen.tellomove.file

import android.content.Context
import android.util.Log
import java.io.File

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
    }
}
