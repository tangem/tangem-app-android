package com.tangem.feature.swap.domain.models.domain

import com.tangem.feature.swap.domain.models.SwapAmount

/**
 * Quote model holds data about current amounts of exchange and fees
 *
 * @property toTokenAmount amount of token you want to receive
 */
data class QuoteModel(
    val toTokenAmount: SwapAmount,
    val allowanceContract: String?,
)