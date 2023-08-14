package com.tangem.data.common.locale

import java.util.Locale

/**
[REDACTED_AUTHOR]
 */
internal class DefaultLocaleProvider : LocaleProvider {

    override fun getLocale(): Locale {
        return Locale.getDefault()
    }

    override fun getWebUriLocaleLanguage(): String {
        val language = getLocale().language
        return if (LOCALE_LANG_RU.equals(language, true) || LOCALE_LANG_BY.equals(language, true)) {
            LOCALE_LANG_RU
        } else {
            LOCALE_LANG_EN
        }
    }

    companion object {
        const val LOCALE_LANG_RU = "ru"
        const val LOCALE_LANG_BY = "by"
        const val LOCALE_LANG_EN = "en"
    }
}