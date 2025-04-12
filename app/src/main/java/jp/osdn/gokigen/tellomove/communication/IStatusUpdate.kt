package jp.osdn.gokigen.tellomove.communication

interface IStatusUpdate
{
    fun updateBatteryRemain(percentage: Int)
    fun updateStatus(status: String)
    fun updateCommandStatus(command: String, isSuccess: Boolean)
    fun queuedCommand(command: String)
}