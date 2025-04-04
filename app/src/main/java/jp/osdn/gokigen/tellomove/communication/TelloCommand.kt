package jp.osdn.gokigen.tellomove.communication

data class TelloCommand(val command: String, val callback: ICommandResult?)
