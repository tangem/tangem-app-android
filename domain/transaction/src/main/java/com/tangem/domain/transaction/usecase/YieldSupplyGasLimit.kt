package com.tangem.domain.transaction.usecase

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.unwrapYieldModuleUpgrade
import com.tangem.utils.isNullOrZero
import com.tangem.utils.logging.TangemLogger
import java.math.BigInteger
import java.math.RoundingMode

private val HUNDRED_PERCENT = 100.toBigInteger() // base 100%
private val INCREASE_GAS_LIMIT_FOR_SUPPLY = 140.toBigInteger() // 40% increase [there is also in Yield FeeExtensions.kt]

/**
 * A send out of the yield module withdraws from the lending protocol, and `eth_estimateGas` for it is too tight
 * to land: sent with the bare estimate, it runs out of gas on chain.
 */
internal fun TransactionFee.fixYieldSupplyGasLimit(transactionData: TransactionData): TransactionFee {
    val uncompiledTransactionData = transactionData as? TransactionData.Uncompiled ?: return this
    val ethereumExtras = uncompiledTransactionData.extras as? EthereumTransactionExtras ?: return this

    return if (ethereumExtras.callData?.unwrapYieldModuleUpgrade() is EthereumYieldSupplySendCallData) {
        val patchedFee = when (this) {
            is TransactionFee.Choosable -> {
                copy(
                    normal = normal.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY),
                    minimum = minimum.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY),
                    priority = priority.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY),
                )
            }
            is TransactionFee.Single -> copy(
                normal = normal.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY),
            )
        }
        TangemLogger.withTag("GAS_FEE_USECASE").i("Fee for Yield Mode adjusted: $patchedFee")
        patchedFee
    } else {
        TangemLogger.withTag("GAS_FEE_USECASE").i("Fee as is: $this")
        this
    }
}

private fun Fee.increaseGasLimitBy(percent: BigInteger): Fee {
    if (this !is Fee.Ethereum) return this
    val gasLimit = gasLimit

    if (gasLimit == BigInteger.ZERO || amount.value.isNullOrZero()) return this

    val increasedGasPrice = amount.value?.movePointRight(amount.decimals)
        ?.divide(gasLimit.toBigDecimal(), RoundingMode.HALF_UP)
    val increasedGasLimit = gasLimit
        .multiply(percent)
        .divide(HUNDRED_PERCENT)
    val increasedAmount = this.amount.copy(
        value = increasedGasLimit.toBigDecimal().multiply(increasedGasPrice).movePointLeft(amount.decimals),
    )
    return when (this) {
        is Fee.Ethereum.TokenCurrency -> error("handle in [REDACTED_TASK_KEY]")
        is Fee.Ethereum.EIP1559 -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
        is Fee.Ethereum.Legacy -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
    }
}