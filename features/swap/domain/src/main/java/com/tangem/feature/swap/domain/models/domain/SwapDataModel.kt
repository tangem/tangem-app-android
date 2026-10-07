package com.tangem.feature.swap.domain.models.domain

import com.tangem.feature.swap.domain.models.SwapAmount
import java.math.BigDecimal

data class SwapDataModel(
    val toTokenAmount: SwapAmount,
    val transaction: ExpressTransactionModel,
    val slippage: BigDecimal? = null,
)