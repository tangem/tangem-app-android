package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.isTronActivationRefusal
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.tron.isPaidByOwnTronEnergy
import com.tangem.domain.transaction.models.tron.isPreferredOverTronGasless
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal

/**
 * Tron gasless: a relayer pays the TRX fee and is compensated out of the token being sent, over two
 * signed transactions instead of an EIP-712 permit. Unlike [EvmGaslessFeeResolver], TRX is weighed against
 * the token fee rather than tried first.
 */
class TronGaslessFeeResolver(
    private val getTronGaslessFeeUseCase: GetTronGaslessFeeUseCase,
    private val isTronGaslessSupportedUseCase: IsTronGaslessSupportedUseCase,
) {

    /**
     * Auto-selected fee currency ([GetFeeForGaslessUseCase]): the fee is quoted in the token the
     * transaction sends.
     *
     * @return `null` when Tron gasless does not apply and the caller's native fee stands. Otherwise
     * [loadNativeFee]'s when TRX is payable and either own energy covers the transfer or it costs no more in
     * fiat than the gasless compensation, else the gasless fee unless only TRX can pay. A refused account
     * activation is returned as is: an account that must be activated first cannot pay in TRX either,
     * so falling back would replace an actionable message with an unpayable fee.
     *
     * [onNativeFeePreferred] fires when TRX is returned in place of an offered or failed gasless quote and
     * the balance covers it, so the caller can keep paying in TRX instead of re-quoting gasless.
     *
     * [sentCurrencyStatus] stands in for the sent token while [accountStatusList] has not caught up with it.
     */
    @Suppress("LongParameterList")
    suspend fun resolveWithAutoFeeToken(
        userWalletId: UserWalletId,
        network: Network,
        transactionData: TransactionData,
        nativeCurrencyStatus: CryptoCurrencyStatus,
        accountStatusList: AccountStatusList,
        sentCurrencyStatus: CryptoCurrencyStatus?,
        loadNativeFee: suspend () -> Either<GetFeeError, TransactionFeeExtended>,
        onNativeFeePreferred: () -> Unit,
    ): Either<GetFeeError, TransactionFeeExtended>? {
        val sentStatus = findSentTokenStatus(
            transactionData = transactionData,
            network = network,
            accountStatusList = accountStatusList,
            fallbackStatus = sentCurrencyStatus,
        ) ?: return null
        val sentToken = sentStatus.currency as CryptoCurrency.Token
        // Not cached: the supported-token check reports a transport failure as "unsupported"
        if (!isTronGaslessSupportedUseCase(userWalletId, network, sentToken)) return null

        val nativeFeeResult = loadNativeFee()
        nativeFeeResult.onRight { nativeFee ->
            // Not reported as preferred: a later destination may need more energy than the account holds.
            if (nativeFee.transactionFee.isPaidByOwnTronEnergy(nativeCurrencyStatus)) return nativeFee.right()
        }

        val gaslessFee = getTronGaslessFeeUseCase(userWalletId, transactionData, sentToken).getOrElse { error ->
            if (error.isTronActivationRefusal()) return error.left()
            TangemLogger.i("Tron gasless quote unavailable ($error), falling back to the native fee")
            return nativeFeeResult.onRight { nativeFee ->
                if (isPayableInNative(nativeCurrencyStatus, nativeFee)) onNativeFeePreferred()
            }
        }

        val nativeFee = nativeFeeResult.getOrElse { return gaslessFee.right() }
        val isNativeCheaper = nativeFee.transactionFee.isPreferredOverTronGasless(
            gaslessFee = gaslessFee.transactionFee,
            nativeStatus = nativeCurrencyStatus,
            gaslessTokenStatus = sentStatus,
        )
        // Not reported as preferred: another amount or destination may change which fee is cheaper.
        if (isNativeCheaper) return nativeFee.right()

        if (coversGaslessFee(sentStatus, gaslessFee)) return gaslessFee.right()

        if (!isPayableInNative(nativeCurrencyStatus, nativeFee)) return gaslessFee.right()

        onNativeFeePreferred()
        return nativeFee.right()
    }

    /**
     * Fee currency picked explicitly by the user ([GetFeeForTokenUseCase]): the fee is quoted in
     * [feeToken], the token being sent.
     *
     * @return the compensation quote, or `null` when Tron gasless does not apply and the caller's own
     * path should run. Unlike [resolveWithAutoFeeToken], a failed quote is not swallowed here:
     * [feeToken] was picked explicitly, so there is no other fee to fall back to.
     */
    suspend fun resolveWithSelectedFeeToken(
        userWalletId: UserWalletId,
        feeToken: CryptoCurrency,
        transactionData: TransactionData,
    ): Either<GetFeeError, TransactionFeeExtended>? {
        if (feeToken !is CryptoCurrency.Token) return null
        if (transactionData.tronGaslessFeeTokenContractOrNull() == null) return null
        if (!isTronGaslessSupportedUseCase(userWalletId, feeToken.network, feeToken)) return null

        return getTronGaslessFeeUseCase(
            userWalletId = userWalletId,
            transactionData = transactionData,
            feeToken = feeToken,
        )
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

    private fun isPayableInNative(nativeStatus: CryptoCurrencyStatus, nativeFee: TransactionFeeExtended): Boolean {
        val feeValue = nativeFee.feeValue() ?: return false
        val nativeBalance = nativeStatus.value.amount ?: return false
        return nativeBalance >= feeValue
    }

    private fun TransactionFeeExtended.feeValue(): BigDecimal? = transactionFee.normal.amount.value

    /**
     * Resolves the token being sent by matching the transaction's contract against wallet currencies.
     *
     * Tron contracts are base58, where case carries meaning, so the match is exact — the same
     * comparison [IsTronGaslessSupportedUseCase] then applies to the token found here. A looser match
     * would hand it a token its own check rejects, silently dropping the gasless quote.
     *
     * The account list is preferred for its fresher balance, with [fallbackStatus] used when the list lags.
     */
    private fun findSentTokenStatus(
        transactionData: TransactionData,
        network: Network,
        accountStatusList: AccountStatusList,
        fallbackStatus: CryptoCurrencyStatus?,
    ): CryptoCurrencyStatus? {
        val contractAddress = transactionData.tronGaslessFeeTokenContractOrNull() ?: return null
        fun CryptoCurrencyStatus.isSentToken(): Boolean {
            val token = currency as? CryptoCurrency.Token ?: return false
            return token.network.id == network.id && token.contractAddress == contractAddress
        }

        return accountStatusList.flattenCurrencies().firstOrNull { it.isSentToken() }
            ?: fallbackStatus?.takeIf { it.isSentToken() }
    }
}