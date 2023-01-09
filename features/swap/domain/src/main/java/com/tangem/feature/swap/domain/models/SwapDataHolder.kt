package com.tangem.feature.swap.domain.models

import com.tangem.feature.swap.domain.models.data.Currency
import com.tangem.feature.swap.domain.models.data.SwapAmount
import com.tangem.feature.swap.domain.models.data.SwapState.QuoteModel
import java.math.BigDecimal

data class SwapDataHolder(
    val quoteModel: QuoteModel? = null,
    val amountToSwap: SwapAmount? = null,
    val networkId: String? = null,
    val exchangeCurrencies: ExchangeCurrencies? = null,
)

data class ExchangeCurrencies(
    val fromCurrency: Currency? = null,
    val toCurrency: Currency? = null,
)