package com.tangem.feature.swap.domain.models.ui

import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import java.math.BigDecimal

sealed class SwapTransactionState {

    data class TxSent(
        val fromAmount: String? = null,
        val fromAmountValue: BigDecimal? = null,
        val toAmount: String? = null,
        val toAmountValue: BigDecimal? = null,
        val txHash: String,
        val txExternalUrl: String? = null,
        val timestamp: Long,
    ) : SwapTransactionState()

    data class TangemPayWithdrawalData(
        val cryptoAmount: BigDecimal,
        val cryptoCurrencyId: CryptoCurrency.RawID,
        val cexAddress: String,
        val fromAmount: String?,
        val fromAmountValue: BigDecimal?,
        val toAmount: String?,
        val toAmountValue: BigDecimal?,
        val storeData: StoreTransactionData,
    ) : SwapTransactionState() {

        data class StoreTransactionData(
            val currencyToSend: CryptoCurrencyStatus,
            val currencyToGet: CryptoCurrencyStatus,
            val fromAccount: Account.CryptoPortfolio?,
            val toAccount: Account.CryptoPortfolio?,
            val amount: SwapAmount,
            val swapProvider: SwapProvider,
            val swapDataModel: SwapDataModel,
            val txExternalUrl: String?,
            val txExternalId: String?,
            val averageDuration: Int?,
        )
    }

    data object DemoMode : SwapTransactionState()

    sealed class Error : SwapTransactionState() {
        data class TransactionError(val error: SendTransactionError?) : Error()

        data class ExpressError(val error: ExpressDataError) : Error()

        data object UnknownError : Error()

        data class TangemPayWithdrawalError(val txId: String) : Error()
    }
}