package jp.osdn.gokigen.tellomove.communication

interface ICommandResult
{
    fun commandResult(command: String, detail: String = "")
}
