package com.tangem.domain.models.currency

import kotlinx.serialization.Serializable

/**
 * Fiat currency in which a [com.tangem.domain.models.quote.QuoteStatus] is expressed. Plain business
 * entity without UI metadata (icons, localized name) — those live in `AppCurrency`.
 *
 * @property code   ISO code, e.g. "USD"
 * @property symbol display symbol, e.g. "$"
 */
@Serializable
data class FiatCurrency(
    val code: String,
    val symbol: String,
) {

    companion object {
        val Default = FiatCurrency(code = "USD", symbol = "$")
    }
}