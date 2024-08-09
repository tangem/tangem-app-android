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
    const val CHINESE = "uk"

    val supportedLangugeCodes = listOf(
        ENGLISH,
        RUSSIAN,
        GERMAN,
        FRANCH,
        ITALIAN,
        JAPANESE,
        UKRAINIAN,
        CHINESE,
    )

    fun getCurrentSupportedLanguageCode(): String {
        val locale = Locale.getDefault()

        return if (supportedLangugeCodes.contains(locale.language)) {
            locale.language
        } else {
            ENGLISH
        }
    }
}