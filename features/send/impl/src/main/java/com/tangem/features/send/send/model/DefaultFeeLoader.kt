package com.tangem.features.send.send.model

import arrow.core.Either
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForGaslessUseCase
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Fee for a send whose fee currency the user has not picked. Once the network coin has been preferred over a
 * compensation fee it is pinned for the rest of the send, so a later reload cannot move the fee row to a
 * token. A coin fee that was never weighed against a compensation fee — an EVM fee or an unsupported token —
 * is not pinned, so the next reload can still offer the token fee.
 */
internal class DefaultFeeLoader @Inject constructor(
    private val getFeeForGaslessUseCase: GetFeeForGaslessUseCase,
    private val getFeeUseCase: GetFeeUseCase,
) {

    /**
     * Taken from the fee the coin was preferred with rather than from the send's fee-currency status: that
     * status starts as, and falls back to, the token being sent.
     */
    private var pinnedCoinId: CryptoCurrency.ID? = null

    suspend fun load(
        userWallet: UserWallet,
        network: Network,
        transactionData: TransactionData,
        sentAmount: BigDecimal?,
        sentCurrencyStatus: CryptoCurrencyStatus,
    ): Either<GetFeeError, TransactionFeeExtended> {
        pinnedCoinId?.let { coinId ->
            return getFeeUseCase(transactionData = transactionData, userWallet = userWallet, network = network)
                .map { fee -> TransactionFeeExtended(transactionFee = fee, feeTokenId = coinId, nativeFee = fee) }
        }

        var isNativeFeePreferred = false
        return getFeeForGaslessUseCase(
            userWallet = userWallet,
            network = network,
            transactionData = transactionData,
            sentAmount = sentAmount,
            sentCurrencyStatus = sentCurrencyStatus,
            onNativeFeePreferred = { isNativeFeePreferred = true },
        ).onRight { fee ->
            if (isNativeFeePreferred) pinToNative(fee.feeTokenId)
        }
    }

    private fun pinToNative(coinId: CryptoCurrency.ID) {
        TangemLogger.i("Paying the fee in the network coin for the rest of this send")
        pinnedCoinId = coinId
    }
}