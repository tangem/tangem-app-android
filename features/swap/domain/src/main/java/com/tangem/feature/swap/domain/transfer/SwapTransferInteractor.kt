package com.tangem.feature.swap.domain.transfer

import arrow.core.Either
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.WithdrawalResult
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import com.tangem.feature.swap.domain.models.ui.SwapState
import java.math.BigDecimal

interface SwapTransferInteractor {

    suspend fun updateTransfer(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        fromTokenAmount: String,
        feePaidCurrencyStatus: CryptoCurrencyStatus?,
        fee: Fee?,
    ): SwapState

    fun shouldTransferInsteadOfSwap(fromSwapCurrency: CryptoCurrency?, toSwapCurrency: CryptoCurrency?): Boolean

    suspend fun loadFee(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        fromTokenAmount: BigDecimal,
    ): Either<GetFeeError, TransactionFee>

    suspend fun loadFeeExtended(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        fromTokenAmount: BigDecimal,
        selectedToken: CryptoCurrencyStatus?,
    ): Either<GetFeeError, TransactionFeeExtended>

    suspend fun sendTransfer(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        sendingAmount: BigDecimal,
        fee: Fee,
        transactionFeeResult: TransactionFeeResult,
    ): Either<SendTransactionError, String>

    suspend fun withdrawTangemPay(
        userWallet: UserWallet,
        cryptoAmount: BigDecimal,
        toSwapCurrencyStatus: SwapCurrencyStatus,
    ): Either<SendTransactionError, WithdrawalResult>

    suspend fun incrementTronTokenFeeShowCount(cryptoCurrencyStatus: CryptoCurrencyStatus?)
}