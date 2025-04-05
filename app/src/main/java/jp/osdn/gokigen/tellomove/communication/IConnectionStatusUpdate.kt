package jp.osdn.gokigen.tellomove.communication

interface IConnectionStatusUpdate
{
    fun setConnectionStatus(isConnect: Boolean)
}