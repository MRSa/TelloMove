package jp.osdn.gokigen.tellomove.communication

interface ICommandResult
{
    fun queuedCommand(command: String)
    fun commandResult(command: String, receivedStatus: Boolean, detail: String = "")
}
