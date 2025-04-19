package jp.osdn.gokigen.tellomove.communication

interface IStatusUpdate
{
    fun updateBatteryRemain(percentage: String)
    fun updateStatus(status: String)
    fun updateCommandStatus(command: String, isSuccess: Boolean, detail: String)
    fun queuedCommand(command: String)
}