package jp.osdn.gokigen.tellomove.speakcommand

interface ISpeakCommandStatusUpdate
{
    fun speechEngineInitialized(status: Boolean)
    fun setSpeakCommandStatus(isEnable: Boolean)

}