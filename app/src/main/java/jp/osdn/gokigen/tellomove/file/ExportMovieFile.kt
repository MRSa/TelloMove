package jp.osdn.gokigen.tellomove.file

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

class ExportMovieFile(private val context: Context)
{
    fun exportBinaryFileExternal(inputFileName: String, outputFileName: String, baseDir :String = "TelloMove"): Boolean
    {
        var outputStream: OutputStream? = null
        try
        {
            var documentUri: Uri? = null
            val outputDir = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path}/$baseDir/"
            val resolver = context.contentResolver
            val extStorageUri: Uri

            val values = ContentValues()
            values.put(MediaStore.Downloads.TITLE, outputFileName)
            values.put(MediaStore.Downloads.DISPLAY_NAME, outputFileName)
            values.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Downloads.RELATIVE_PATH, "Download/$baseDir")
                values.put(MediaStore.Downloads.IS_PENDING, true)
                extStorageUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                Log.v(TAG, "---------- $outputFileName $values")

                documentUri = resolver.insert(extStorageUri, values)
                if (documentUri != null)
                {
                    val uriData : Uri = documentUri
                    outputStream = resolver.openOutputStream(uriData, "wa")
                }
                else
                {
                    return (false)
                }
            }
            else
            {
                val path = File(outputDir)
                path.mkdir()
                values.put(MediaStore.Downloads.DATA, path.absolutePath + File.separator + outputFileName)
                val targetFile = File(outputDir + File.separator + outputFileName)
                outputStream = FileOutputStream(targetFile)
            }
            if (outputStream != null)
            {
                try
                {
                    val localDir = context.getExternalFilesDir(null)
                    val filePath = "${localDir?.absolutePath}/$baseDir/$inputFileName"
                    val fileInputStream = FileInputStream(File(filePath))
                    fileInputStream.copyTo(outputStream)
                    fileInputStream.close()
                }
                catch (e: Exception)
                {
                    e.printStackTrace()
                }
            }
            else
            {
                // 作成した空のファイルを削除することも検討
                if (documentUri != null)
                {
                    resolver.delete(documentUri, null, null)
                }
                return (false)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                values.put(MediaStore.Downloads.IS_PENDING, false)
                if (documentUri != null)
                {
                    val myUri: Uri = documentUri
                    resolver.update(myUri, values, null, null)
                }
            }
            return (true)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
        finally {
            outputStream?.close()
        }
        return (false)
    }

    companion object
    {
        private val  TAG = ExportMovieFile::class.java.simpleName
    }
}
