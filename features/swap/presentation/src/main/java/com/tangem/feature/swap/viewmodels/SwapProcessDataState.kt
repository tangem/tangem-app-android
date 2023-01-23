package com.tangem.feature.swap.viewmodels

import com.tangem.feature.swap.domain.models.domain.ApproveModel
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.domain.SwapDataModel

data class SwapProcessDataState(
    val networkId: String,
    val fromCurrency: Currency? = null,
    val toCurrency: Currency? = null,
    val amount: String? = null,
    val estimatedGas: Int? = null,
    val approveModel: ApproveModel? = null,
    val swapModel: SwapDataModel? = null,
)
