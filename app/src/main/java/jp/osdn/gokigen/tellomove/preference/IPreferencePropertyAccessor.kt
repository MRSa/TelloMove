package jp.osdn.gokigen.tellomove.preference

interface IPreferencePropertyAccessor
{

    companion object
    {
        // --- PREFERENCE KEY AND DEFAULT VALUE ---
        const val PREFERENCE_USE_WATCHDOG = "use_watchdog"
        const val PREFERENCE_USE_WATCHDOG_DEFAULT_VALUE = true

        const val PREFERENCE_SPEAK_COMMANDS = "speak_commands"
        const val PREFERENCE_SPEAK_COMMANDS_DEFAULT_VALUE = false

    }
}