package com.tangem.data.common.locale

import java.util.Locale

/**
 * @author Anton Zhilenkov on 12.08.2023.
 */
interface LocaleProvider {

    fun getLocale(): Locale

    fun getWebUriLocaleLanguage(): String
}
