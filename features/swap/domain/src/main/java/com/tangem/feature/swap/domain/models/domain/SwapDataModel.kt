package com.tangem.feature.swap.domain.models.domain

import com.tangem.feature.swap.domain.models.SwapAmount

data class SwapDataModel(
    val toTokenAmount: SwapAmount,
    val transaction: ExpressTransactionModel,
)