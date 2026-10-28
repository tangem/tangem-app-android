package com.tangem.domain.transaction.usecase.gasless

import arrow.core.raise.Raise
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.raiseIllegalStateError
import java.math.BigDecimal

/** Shared by the auto and the manual fee-token paths so both produce identical plans. */
@Suppress("LongParameterList")
internal suspend fun Raise<GetFeeError>.attachGaslessFeePlan(
    resolveGaslessFeePlanUseCase: ResolveGaslessFeePlanUseCase,
    userWallet: UserWallet,
    tokenStatus: CryptoCurrencyStatus,
    tokenFeeExtended: TransactionFeeExtended,
    transactionData: TransactionData,
    isYieldActive: Boolean,
    sentAmount: BigDecimal? = null,
): TransactionFeeExtended {
    val feeTokenContract = (tokenStatus.currency as? CryptoCurrency.Token)?.contractAddress
        ?: raiseIllegalStateError("gasless fee currency must be a token")

    return attachGaslessFeePlan(
        resolveGaslessFeePlanUseCase = resolveGaslessFeePlanUseCase,
        userWallet = userWallet,
        tokenStatus = tokenStatus,
        tokenFeeExtended = tokenFeeExtended,
        sendAmountInFeeToken = computeSendAmountInFeeToken(transactionData, feeTokenContract, sentAmount),
        isYieldActive = isYieldActive,
    )
}

/** For the estimate paths, where no transaction is built yet — a CEX swap only knows the amount it will send. */
@Suppress("LongParameterList")
internal suspend fun Raise<GetFeeError>.attachGaslessFeePlan(
    resolveGaslessFeePlanUseCase: ResolveGaslessFeePlanUseCase,
    userWallet: UserWallet,
    tokenStatus: CryptoCurrencyStatus,
    tokenFeeExtended: TransactionFeeExtended,
    sendAmountInFeeToken: BigDecimal,
    isYieldActive: Boolean,
): TransactionFeeExtended {
    val feeInTokenCurrency = tokenFeeExtended.transactionFee.normal as? Fee.Ethereum.TokenCurrency
        ?: raiseIllegalStateError("gasless token fee must be Fee.Ethereum.TokenCurrency")

    val plan = resolveGaslessFeePlanUseCase(
        userWallet = userWallet,
        tokenStatus = tokenStatus,
        tokenFee = feeInTokenCurrency,
        isYieldActive = isYieldActive,
        sendAmountInFeeToken = sendAmountInFeeToken,
    ).bind()

    return tokenFeeExtended.copy(gaslessFeePlan = plan)
}

/**
 * How much of the fee token the main transaction also spends, counted into the required-balance check.
 *
 * A compiled tx or a missing amount on the matching-token path are programmer errors and raise, rather
 * than under-account as ZERO.
 */
internal fun Raise<GetFeeError>.computeSendAmountInFeeToken(
    transactionData: TransactionData,
    feeTokenContract: String,
    sentAmount: BigDecimal? = null,
): BigDecimal {
    // Gasless token-fee requires uncompiled tx data (mirrors CreateAndSendGaslessTransactionUseCase).
    val uncompiled = transactionData as? TransactionData.Uncompiled
        ?: raiseIllegalStateError("gasless token fee requires uncompiled transaction data")
    val sentTokenContract = when (val type = uncompiled.amount.type) {
        is AmountType.Token -> type.token.contractAddress
        is AmountType.TokenYieldSupply -> type.token.contractAddress
        else -> null
    }
    if (sentTokenContract == null || !sentTokenContract.equals(feeTokenContract, ignoreCase = true)) {
        return BigDecimal.ZERO
    }

    // A yield-supply send keeps the real amount in the module call data and zeroes TransactionData.amount.
    // Reading that zero back would make the resolver skip the yield withdraw, and the executor would revert
    // on an EOA the send has just emptied.
    if (uncompiled.isYieldSupplySend()) {
        return sentAmount
            ?: raiseIllegalStateError("sent amount is required to pay the gasless fee in a yield-supply send")
    }

    return sentAmount
        ?: uncompiled.amount.value
        ?: raiseIllegalStateError("sent amount is null while paying the gasless fee in the sent token")
}

private fun TransactionData.Uncompiled.isYieldSupplySend(): Boolean {
    return (extras as? EthereumTransactionExtras)?.callData is EthereumYieldSupplySendCallData
}

internal fun computeSendAmountInFeeToken(
    sendingCurrency: CryptoCurrency,
    feeTokenContract: String,
    amount: BigDecimal,
): BigDecimal {
    val sentTokenContract = (sendingCurrency as? CryptoCurrency.Token)?.contractAddress
    return if (sentTokenContract != null && sentTokenContract.equals(feeTokenContract, ignoreCase = true)) {
        amount
    } else {
        BigDecimal.ZERO
    }
}