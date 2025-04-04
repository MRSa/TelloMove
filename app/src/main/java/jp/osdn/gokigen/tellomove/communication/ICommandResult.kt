package jp.osdn.gokigen.tellomove.communication

interface ICommandResult
{
    fun commandResult(result: Boolean, detail: String = "")
}
