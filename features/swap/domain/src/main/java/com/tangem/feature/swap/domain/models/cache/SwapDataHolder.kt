package com.tangem.feature.swap.domain.models.cache

import com.tangem.feature.swap.domain.models.domain.ApproveModel
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.domain.QuoteModel
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.SwapDataModel

data class SwapDataHolder(
    val quoteModel: QuoteModel? = null,
    val swapModel: SwapDataModel? = null,
    val approveTxModel: ApproveModel? = null,
    val amountToSwap: SwapAmount? = null,
    val networkId: String? = null,
    val exchangeCurrencies: ExchangeCurrencies? = null,
)

data class ExchangeCurrencies(
    val fromCurrency: Currency? = null,
    val toCurrency: Currency? = null,
)
