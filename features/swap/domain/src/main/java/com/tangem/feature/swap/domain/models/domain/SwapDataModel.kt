package com.tangem.feature.swap.domain.models.domain

import com.tangem.feature.swap.domain.models.SwapAmount

/**
 * Swap transaction model
 *
 * @property toTokenAmount amount "target" token
 * @property transaction info about transaction
 */
data class SwapDataModel(
    val toTokenAmount: SwapAmount,
    val transaction: TransactionModel,
)