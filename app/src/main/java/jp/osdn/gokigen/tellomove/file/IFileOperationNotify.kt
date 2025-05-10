package jp.osdn.gokigen.tellomove.file

interface IFileOperationNotify
{
    fun onCompletedExport(result: Boolean, fileName: String)
}