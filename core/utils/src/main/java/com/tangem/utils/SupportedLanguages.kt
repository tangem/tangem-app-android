package com.tangem.utils

import java.util.Locale

object SupportedLanguages {
    const val LANG_ENGLISH = "en"
    const val LANG_RUSSIAN = "ru"
    const val LANG_GERMAN = "de"
    const val LANG_FRANCH = "fr"
    const val LANG_ITALIAN = "it"
    const val LANG_JAPANESE = "ja"
    const val LANG_UKRAINIAN = "uk"
    const val LANG_CHINESE = "uk"

    val supportedLangugeCodes = listOf(
        LANG_ENGLISH,
        LANG_RUSSIAN,
        LANG_GERMAN,
        LANG_FRANCH,
        LANG_ITALIAN,
        LANG_JAPANESE,
        LANG_UKRAINIAN,
        LANG_CHINESE,
    )

    fun getCurrentSupportedLanguageCode(): String {
        val locale = Locale.getDefault()

        return if (supportedLangugeCodes.contains(locale.language)) {
            locale.language
        } else {
            LANG_ENGLISH
        }
    }
}
