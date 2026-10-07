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
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal

/**
 * Tron gasless: a relayer pays the TRX fee and is compensated out of the token being sent, over two
 * signed transactions instead of an EIP-712 permit. Unlike [EvmGaslessFeeResolver], the token is tried
 * first and TRX is the fallback.
 */
class TronGaslessFeeResolver(
    private val getTronGaslessFeeUseCase: GetTronGaslessFeeUseCase,
    private val isTronGaslessSupportedUseCase: IsTronGaslessSupportedUseCase,
) {

    /**
     * Auto-selected fee currency ([GetFeeForGaslessUseCase]): the fee is quoted in the token the
     * transaction sends.
     *
     * @return `null` when Tron gasless does not apply and the caller's native fee stands. Otherwise the
     * gasless fee, or [loadNativeFee]'s when TRX turns out to be the better payer. A refused account
     * activation is returned as is: an account that must be activated first cannot pay in TRX either,
     * so falling back would replace an actionable message with an unpayable fee.
     *
     * [onNativeFeePreferred] fires when TRX is returned in place of an offered or failed gasless quote and
     * the balance covers it, so the caller can keep paying in TRX instead of re-quoting gasless.
     */
    @Suppress("LongParameterList")
    suspend fun resolveWithAutoFeeToken(
        userWalletId: UserWalletId,
        network: Network,
        transactionData: TransactionData,
        nativeCurrencyStatus: CryptoCurrencyStatus,
        accountStatusList: AccountStatusList,
        loadNativeFee: suspend () -> Either<GetFeeError, TransactionFeeExtended>,
        onNativeFeePreferred: () -> Unit,
    ): Either<GetFeeError, TransactionFeeExtended>? {
        val sentStatus = findSentTokenStatus(transactionData, network, accountStatusList) ?: return null
        val sentToken = sentStatus.currency as CryptoCurrency.Token
        // Not cached: the supported-token check reports a transport failure as "unsupported"
        if (!isTronGaslessSupportedUseCase(userWalletId, network, sentToken)) return null

        val gaslessFee = getTronGaslessFeeUseCase(userWalletId, transactionData, sentToken).getOrElse { error ->
            if (error.isTronActivationRefusal()) return error.left()
            TangemLogger.i("Tron gasless quote unavailable ($error), falling back to the native fee")
            return loadNativeFee().onRight { nativeFee ->
                if (isPayableInNative(nativeCurrencyStatus, nativeFee)) onNativeFeePreferred()
            }
        }

        if (coversGaslessFee(sentStatus, gaslessFee)) return gaslessFee.right()

        val nativeFee = loadNativeFee().getOrElse { return gaslessFee.right() }
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
        if (transactionData.tronGaslessTokenContractOrNull() == null) return null
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
     */
    private fun findSentTokenStatus(
        transactionData: TransactionData,
        network: Network,
        accountStatusList: AccountStatusList,
    ): CryptoCurrencyStatus? {
        val contractAddress = transactionData.tronGaslessTokenContractOrNull() ?: return null
        return accountStatusList.flattenCurrencies().firstOrNull { status ->
            val currency = status.currency
            currency is CryptoCurrency.Token &&
                currency.network.id == network.id &&
                currency.contractAddress == contractAddress
        }
    }
}