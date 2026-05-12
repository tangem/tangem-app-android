package com.tangem.feature.swap.domain.transfer

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.feature.swap.domain.models.ui.SwapState

interface SwapTransferInteractor {

    suspend fun updateTransfer(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        fromTokenAmount: String,
    ): SwapState

    fun shouldTransferInsteadOfSwap(fromSwapCurrency: CryptoCurrency, toSwapCurrency: CryptoCurrency): Boolean
}