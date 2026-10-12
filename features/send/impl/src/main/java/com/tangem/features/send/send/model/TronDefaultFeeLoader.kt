package com.tangem.features.send.send.model

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.isTronActivationRefusal
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.gasless.GetTronGaslessFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.IsTronGaslessSupportedUseCase
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Default fee token for a Tron send. TRX is pinned for the rest of the send only once it is known to cover
 * the native fee: pinning it otherwise would strand a wallet with no TRX on a fee it cannot pay.
 */
internal class TronDefaultFeeLoader @Inject constructor(
    private val getTronGaslessFeeUseCase: GetTronGaslessFeeUseCase,
    private val isTronGaslessSupportedUseCase: IsTronGaslessSupportedUseCase,
) {

    private var isPinnedToNative = false

    suspend fun isGaslessAvailable(
        userWalletId: UserWalletId,
        network: Network,
        feeToken: CryptoCurrency.Token,
    ): Boolean = isTronGaslessSupportedUseCase(userWalletId, network, feeToken)

    suspend fun load(
        userWalletId: UserWalletId,
        sentStatus: CryptoCurrencyStatus,
        nativeStatus: CryptoCurrencyStatus,
        transactionData: TransactionData,
        loadNativeFee: suspend () -> Either<GetFeeError, TransactionFeeExtended>,
    ): Either<GetFeeError, TransactionFeeExtended> {
        if (isPinnedToNative) return loadNativeFee()

        // Not cached: the supported-token check reports a transport failure as "unsupported".
        val sentToken = sentStatus.currency as? CryptoCurrency.Token
        if (sentToken == null || !isGaslessAvailable(userWalletId, sentToken.network, sentToken)) {
            return loadNativeFee()
        }

        return loadGaslessFee(
            quoteGaslessFee = { getTronGaslessFeeUseCase(userWalletId, transactionData, sentToken) },
            sentStatus = sentStatus,
            nativeStatus = nativeStatus,
            loadNativeFee = loadNativeFee,
        )
    }

    private suspend fun loadGaslessFee(
        quoteGaslessFee: suspend () -> Either<GetFeeError, TransactionFeeExtended>,
        sentStatus: CryptoCurrencyStatus,
        nativeStatus: CryptoCurrencyStatus,
        loadNativeFee: suspend () -> Either<GetFeeError, TransactionFeeExtended>,
    ): Either<GetFeeError, TransactionFeeExtended> {
        val gaslessFee = quoteGaslessFee().getOrElse { error ->
            // An account that must be activated first cannot pay a native fee either — falling back
            // would replace an actionable message with an unpayable TRX fee.
            if (error.isTronActivationRefusal()) return error.left()
            TangemLogger.i("Tron gasless quote unavailable ($error), falling back to the native fee")
            return loadNativeFee().onRight { pinToNativeIfPayable(nativeStatus, it) }
        }

        if (coversGaslessFee(sentStatus, gaslessFee)) return gaslessFee.right()

        val nativeFee = loadNativeFee().getOrElse { return gaslessFee.right() }
        return if (pinToNativeIfPayable(nativeStatus, nativeFee)) nativeFee.right() else gaslessFee.right()
    }

    /**
     * Only the compensation itself is weighed against the balance: a balance that pays the fee but not the
     * whole transfer keeps the gasless fee, and the send reduces the transferred amount by that fee instead.
     * The comparison is strict because a balance equal to the fee leaves nothing to transfer.
     */
    private fun coversGaslessFee(sentStatus: CryptoCurrencyStatus, gaslessFee: TransactionFeeExtended): Boolean {
        val compensation = gaslessFee.feeValue() ?: return true
        val sentBalance = sentStatus.value.amount ?: return true
        return sentBalance > compensation
    }

    private fun pinToNativeIfPayable(nativeStatus: CryptoCurrencyStatus, nativeFee: TransactionFeeExtended): Boolean {
        // The fee status falls back to the sent token when the fee-paid currency is unresolved.
        if (nativeStatus.currency !is CryptoCurrency.Coin) return false
        val feeValue = nativeFee.feeValue() ?: return false
        val nativeBalance = nativeStatus.value.amount ?: return false
        if (nativeBalance < feeValue) return false

        TangemLogger.i("Paying the Tron fee in the network coin for the rest of this send")
        isPinnedToNative = true
        return true
    }

    private fun TransactionFeeExtended.feeValue(): BigDecimal? = transactionFee.normal.amount.value
}