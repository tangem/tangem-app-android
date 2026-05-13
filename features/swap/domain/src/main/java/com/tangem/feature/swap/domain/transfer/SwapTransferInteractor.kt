package com.tangem.feature.swap.domain.transfer

import arrow.core.Either
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.feature.swap.domain.models.ui.SwapState

interface SwapTransferInteractor {

    suspend fun updateTransfer(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        fromTokenAmount: String,
    ): SwapState

    fun shouldTransferInsteadOfSwap(fromSwapCurrency: CryptoCurrency?, toSwapCurrency: CryptoCurrency?): Boolean

    suspend fun loadFee(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        fromTokenAmount: String,
    ): Either<GetFeeError, TransactionFee>

    suspend fun loadFeeExtended(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        fromTokenAmount: String,
    ): Either<GetFeeError, TransactionFeeExtended>
}