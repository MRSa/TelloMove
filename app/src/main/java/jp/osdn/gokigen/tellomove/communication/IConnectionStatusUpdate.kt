package jp.osdn.gokigen.tellomove.communication

interface IConnectionStatusUpdate
{
    fun queuedConnectionCommand(command: String)
    fun setConnectionStatus(isConnect: Boolean)
}