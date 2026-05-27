package com.tangem.utils

import java.util.Locale

object SupportedLanguages {
    const val ENGLISH = "en"
    const val RUSSIAN = "ru"
    const val GERMAN = "de"
    const val FRANCH = "fr"
    const val ITALIAN = "it"
    const val JAPANESE = "ja"
    const val UKRAINIAN = "uk"
    const val CHINESE = "zh"
    const val SPANISH = "es"

    val supportedLanguageCodes = listOf(
        ENGLISH,
        RUSSIAN,
        GERMAN,
        FRANCH,
        ITALIAN,
        JAPANESE,
        UKRAINIAN,
        CHINESE,
        SPANISH,
    )

    /**
     * Returns the ISO 639-1 code of the device's current language when it belongs to
     * [supportedLanguageCodes], otherwise falls back to [ENGLISH].
     *
     * Intended for callers that need a plain two-letter language code (e.g. URL path segments
     * like `tangem.com/{en|ru}/...`).
     */
    fun getCurrentSupportedLanguageCode(): String {
        val locale = Locale.getDefault()

        return if (supportedLanguageCodes.contains(locale.language)) {
            locale.language
        } else {
            ENGLISH
        }
    }
}