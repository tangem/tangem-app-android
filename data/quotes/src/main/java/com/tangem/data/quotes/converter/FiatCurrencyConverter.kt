package com.tangem.data.quotes.converter

import com.tangem.data.quotes.store.QuoteStatusDM
import com.tangem.domain.models.currency.FiatCurrency
import com.tangem.utils.converter.TwoWayConverter

/**
 * Two-way converter between domain [FiatCurrency] and persisted [QuoteStatusDM.FiatCurrency].
 *
 * - [convert] — domain → DM (for persistence).
 * - [convertBack] — DM → domain (for restore on cold start).
 */
internal object FiatCurrencyConverter : TwoWayConverter<FiatCurrency, QuoteStatusDM.FiatCurrency> {

    override fun convert(value: FiatCurrency): QuoteStatusDM.FiatCurrency =
        QuoteStatusDM.FiatCurrency(code = value.code, symbol = value.symbol)

    override fun convertBack(value: QuoteStatusDM.FiatCurrency): FiatCurrency =
        FiatCurrency(code = value.code, symbol = value.symbol)
}