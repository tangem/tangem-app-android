package com.tangem.feature.swap.viewmodels

import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.ui.RequestApproveStateData
import com.tangem.feature.swap.domain.models.ui.SwapStateData
import com.tangem.feature.swap.domain.models.ui.TxFee

data class SwapProcessDataState(
    val networkId: String,
    val fromCurrency: Currency? = null,
    val toCurrency: Currency? = null,
    val amount: String? = null,
    val approveDataModel: RequestApproveStateData? = null,
    val swapDataModel: SwapStateData? = null,
    val selectedFee: TxFee? = null,
)