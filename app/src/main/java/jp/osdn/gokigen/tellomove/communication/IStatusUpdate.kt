package jp.osdn.gokigen.tellomove.communication

interface IStatusUpdate
{
    fun updateBatteryRemain(percentage: Int)
    fun updateStatus(status: String)
}