package com.tangem.utils

import java.util.Locale

object TangemHelpCenterUrlBuilder {

    fun siteLanguage(locale: Locale = Locale.getDefault()): String = when (val language = locale.language) {
        "de", "es", "fr", "ja", "ko", "pt", "uk" -> language
        "zh" -> "zh-Hans"
        else -> SupportedLanguages.ENGLISH
    }

    fun helpCenterUrl(articlePath: String, anchor: String? = null, locale: Locale = Locale.getDefault()): String {
        val fragment = anchor?.let { "#$it" }.orEmpty()
        return "https://tangem.com/${siteLanguage(locale)}/help-center/${articlePath.trim('/')}/$fragment"
    }
}