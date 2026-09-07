package com.tangem.features.send.send.model

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.gasless.GetTronGaslessFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.IsTronGaslessSupportedUseCase
import com.tangem.features.send.api.SendFeatureToggles
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
    private val sendFeatureToggles: SendFeatureToggles,
) {

    private var isPinnedToNative = false

    suspend fun isGaslessAvailable(
        userWalletId: UserWalletId,
        network: Network,
        feeToken: CryptoCurrency.Token,
    ): Boolean =
        sendFeatureToggles.isTronGaslessEnabled && isTronGaslessSupportedUseCase(userWalletId, network, feeToken)

    @Suppress("LongParameterList")
    suspend fun load(
        userWalletId: UserWalletId,
        sentStatus: CryptoCurrencyStatus,
        nativeStatus: CryptoCurrencyStatus,
        transactionData: TransactionData,
        sentAmount: BigDecimal?,
        loadNativeFee: suspend () -> Either<GetFeeError, TransactionFeeExtended>,
    ): Either<GetFeeError, TransactionFeeExtended> {
        if (isPinnedToNative) return loadNativeFee()

        // Not cached: the supported-token check reports a transport failure as "unsupported".
        val sentToken = sentStatus.currency as? CryptoCurrency.Token
        if (sentToken == null || !isGaslessAvailable(userWalletId, sentToken.network, sentToken)) {
            return loadNativeFee()
        }

        return loadGaslessFee(
            token = sentToken,
            sentStatus = sentStatus,
            nativeStatus = nativeStatus,
            transactionData = transactionData,
            sentAmount = sentAmount,
            loadNativeFee = loadNativeFee,
        )
    }

    @Suppress("LongParameterList")
    private suspend fun loadGaslessFee(
        token: CryptoCurrency.Token,
        sentStatus: CryptoCurrencyStatus,
        nativeStatus: CryptoCurrencyStatus,
        transactionData: TransactionData,
        sentAmount: BigDecimal?,
        loadNativeFee: suspend () -> Either<GetFeeError, TransactionFeeExtended>,
    ): Either<GetFeeError, TransactionFeeExtended> {
        val gaslessFee = getTronGaslessFeeUseCase(transactionData, token).getOrElse { error ->
            TangemLogger.i("Tron gasless quote unavailable ($error), falling back to the native fee")
            return loadNativeFee().onRight { pinToNativeIfPayable(nativeStatus, it) }
        }

        if (!isSentTokenShort(sentStatus, sentAmount, gaslessFee)) return gaslessFee.right()

        val nativeFee = loadNativeFee().getOrElse { return gaslessFee.right() }
        return if (pinToNativeIfPayable(nativeStatus, nativeFee)) nativeFee.right() else gaslessFee.right()
    }

    private fun isSentTokenShort(
        sentStatus: CryptoCurrencyStatus,
        sentAmount: BigDecimal?,
        gaslessFee: TransactionFeeExtended,
    ): Boolean {
        val compensation = gaslessFee.feeValue() ?: return false
        val sentBalance = sentStatus.value.amount ?: return false
        return sentAmount != null && sentBalance < sentAmount + compensation
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