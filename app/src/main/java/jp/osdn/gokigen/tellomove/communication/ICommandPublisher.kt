package jp.osdn.gokigen.tellomove.communication

interface ICommandPublisher
{
    fun enqueueCommand(command: String, callback: ICommandResult?): Boolean
    fun isConnected(): Boolean
    fun start()
    fun stop()
}