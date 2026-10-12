package com.tangem.features.txhistory.converter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import java.math.BigDecimal

/**
 * Prices a crypto amount of the viewed currency in fiat: its live [rate] per unit, quoted in [appCurrency].
 *
 * [rate] is `null` whenever there is nothing to convert with — the quote has not arrived yet, or the currency has no
 * price at all (a custom token the backend knows no quote for). [convert] is then `null` too, so the caller omits the
 * fiat line rather than stating a zero.
 */
internal data class FiatConversion(val rate: BigDecimal?, val appCurrency: AppCurrency) {

    fun convert(cryptoAmount: BigDecimal): TextReference? {
        val fiatAmount = rate?.multiply(cryptoAmount) ?: return null

        return stringReference(
            fiatAmount.format { fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol) },
        )
    }

    companion object {

        /** No rate known — every conversion yields `null`. */
        val NONE = FiatConversion(rate = null, appCurrency = AppCurrency.Default)
    }
}