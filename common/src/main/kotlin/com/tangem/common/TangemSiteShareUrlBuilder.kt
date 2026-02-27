package com.tangem.common

import com.tangem.utils.SupportedLanguages.CHINESE
import com.tangem.utils.SupportedLanguages.ENGLISH
import com.tangem.utils.SupportedLanguages.FRANCH
import com.tangem.utils.SupportedLanguages.GERMAN
import com.tangem.utils.SupportedLanguages.JAPANESE
import java.util.Locale

object TangemSiteShareUrlBuilder {

    private const val BASE_URL = "https://tangem.com"
    private const val CRYPTOCURRENCIES_PATH = "cryptocurrencies"

    @Deprecated("Should use CHINESE from SupportedLanguages, but the site expects zh-Hans in the URL path")
    private const val CHINESE_SITE_LOCALE = "zh-Hans"

    @Deprecated("Should rely on SupportedLanguages instead of maintaining a separate list")
    private val siteLocales = mapOf(
        ENGLISH to ENGLISH,
        FRANCH to FRANCH,
        GERMAN to GERMAN,
        JAPANESE to JAPANESE,
        CHINESE to CHINESE_SITE_LOCALE,
    )

    fun shareUrl(tokenId: String): String {
        val locale = siteLocales[Locale.getDefault().language] ?: ENGLISH
        return "$BASE_URL/$locale/$CRYPTOCURRENCIES_PATH/$tokenId"
    }
}