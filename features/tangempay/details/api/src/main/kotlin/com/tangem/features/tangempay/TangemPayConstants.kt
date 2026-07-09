package com.tangem.features.tangempay

import com.tangem.utils.SupportedLanguages

object TangemPayConstants {
    const val TERMS_AND_LIMITS_LINK = "https://tangem.com/docs/en/tangem-visa-tariffs.pdf"

    fun visaBenefitsLink(): String {
        val lang = SupportedLanguages.getCurrentSupportedLanguageCode()
        val segment = when (lang) {
            "es" -> "es"
            "pt" -> "pt"
            "ja" -> "ja"
            "zh" -> "zh-Hans"
            "fr" -> "fr"
            "de" -> "de"
            else -> "en"
        }
        return "https://tangem.com/$segment/tangem-pay/visa-benefits/"
    }
}