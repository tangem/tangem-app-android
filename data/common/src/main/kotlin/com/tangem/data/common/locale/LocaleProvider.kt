package com.tangem.data.common.locale

import java.util.Locale

/**
[REDACTED_AUTHOR]
 */
interface LocaleProvider {

    fun getLocale(): Locale

    fun getWebUriLocaleLanguage(): String
}