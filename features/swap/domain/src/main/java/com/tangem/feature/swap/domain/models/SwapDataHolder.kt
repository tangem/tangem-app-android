package com.tangem.feature.swap.domain.models

import com.tangem.feature.swap.domain.models.data.SwapState.QuoteModel

data class SwapDataHolder(
    val quoteModel:
    QuoteModel? = null,
    val amountToSwap: String? = null,
    val networkId: String? = null,
)
