package com.tangem.feature.swap.domain.models

import com.tangem.feature.swap.domain.models.data.QuoteModel

data class SwapDataHolder(
    val quoteModel: QuoteModel? = null,
    val amountToSwap: String? = null,
    val networkId: String? = null,
)