package com.tangem.domain.apptheme.model

/**
 * Enumerates the possible modes for the application's theme.
 */
enum class AppThemeMode(val value: String) {
    /**
     * Forces the dark theme mode regardless of system settings.
     */
    FORCE_DARK("Dark"),

    /**
     * Forces the light theme mode regardless of system settings.
     */
    FORCE_LIGHT("Light"),

    /**
     * Follows the system-wide theme mode.
     */
    FOLLOW_SYSTEM("System"),

    ;

    companion object {
        /**
         * The default [AppThemeMode].
         */
        val DEFAULT: AppThemeMode = FOLLOW_SYSTEM

        /**
         * List of available [AppThemeMode]s.
         * */
        val available: List<AppThemeMode> = entries
    }
}