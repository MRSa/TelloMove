package jp.osdn.gokigen.tellomove.speakcommand

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.util.Log

class SpeakTelloCommand(context: Context, private val callback: ISpeakCommandStatusUpdate): TextToSpeech.OnInitListener
{
    private var textToSpeech: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(p0: Int)
    {
        try
        {

            if (p0 == TextToSpeech.SUCCESS)
            {
                textToSpeech.let { tts ->
                    val locale = Locale.JAPAN
                    if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE)
                    {
                        tts.language = Locale.JAPAN
                        tts.setLanguage(locale)
                        isReady = true
                        Log.v(TAG, "TTS IS INITIALIZED.")
                    }
                    else
                    {
                        Log.v(TAG, "ERROR> ERROR SET TTS LANGUAGE(JAPANESE).")
                        isReady = false
                    }
                }
            }
            else
            {
                Log.v(TAG, "ERROR> TTS INITIALIZE FAILURE. $p0")
                isReady = false
            }
            callback.speechEngineInitialized(isReady)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
            isReady = false
        }
    }

    fun speakTelloCommand(telloCommand: String)
    {
        try
        {
            val keyword = when (telloCommand) {
                "streamon" -> "録画終了"
                "streamoff" -> "録画開始"
                "rec_start" -> "録画開始"
                "rec_stop" -> "録画終了"
                "takeoff" -> "離陸"
                "land" -> "着陸"
                "command" -> "コマンド"
                "emergency" -> "緊急"
                "stop" -> "バージョン"
                else -> decideKeywordString(telloCommand)
            }
            if (keyword.isNotEmpty())
            {
                Log.v(TAG, "===== 　SPEAK : $keyword ($telloCommand)　=====")
                speakText(keyword)
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    private fun decideKeywordString(telloCommand: String) : String
    {
        try
        {
            var operation = ""
            var numValue = 0
            val command = telloCommand.split(" ")
            if (command.isNotEmpty())
            {
                operation = command[0]
            }
            if (command.size >= 2)
            {
                numValue = try { command[1].toInt() } catch (ee: Exception) { 0 }
            }
            val word = when (operation)
            {
                "up" -> { if (numValue < 70) { "すこし上" } else { "うえ" }}
                "down" -> { if (numValue < 70) { "すこし下" } else { "した" }}
                "left" -> { if (numValue < 70) { "少しっ左" } else { "ひだり" }}
                "right" -> { if (numValue < 70) { "すこしみぎぃ" } else { "みぎ" }}
                "forward" -> { if (numValue < 70) { "すこし前" } else { "まえっ" }}
                "back" -> { if (numValue < 70) { "すこし後ろ" } else { "後ろ" }}
                "cw" -> { if (numValue < 60) { "ななめ右" } else { "右向き" }}
                "ccw" -> { if (numValue < 60) { "ななめ左" } else { "左向き" }}
                else -> { "" }
            }
            return (word)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
        return ("")
    }

    private fun speakText(text: String)
    {
        try
        {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "telloMove")
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    fun onDestroy()
    {
        try
        {
            Log.v(TAG, "onDestroy()")
            textToSpeech.shutdown()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    companion object
    {
        private val TAG = SpeakTelloCommand::class.java.simpleName
    }
}
