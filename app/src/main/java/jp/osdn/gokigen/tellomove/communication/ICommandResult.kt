package jp.osdn.gokigen.tellomove.communication

interface ICommandResult
{
    fun commandResult(command: String, receivedStatus: Boolean, detail: String = "")
}
