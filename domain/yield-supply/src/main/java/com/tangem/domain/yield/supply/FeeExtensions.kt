package com.tangem.domain.yield.supply

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigInteger
import java.math.RoundingMode

private val HUNDRED_PERCENT = 100.toBigInteger() // base 100%
val INCREASE_GAS_LIMIT_FOR_SUPPLY = 120.toBigInteger() // 20% increase

fun Fee.fixFee(cryptoCurrency: CryptoCurrency, gasLimit: BigInteger): Fee = when (this) {
    is Fee.Ethereum.Legacy -> copy(
        gasLimit = gasLimit,
        amount = amount.copy(
            value = gasPrice.multiply(gasLimit)
                .toBigDecimal().movePointLeft(cryptoCurrency.decimals),
        ),
    )
    is Fee.Ethereum.EIP1559 -> copy(
        gasLimit = gasLimit,
        amount = amount.copy(
            value = maxFeePerGas.multiply(gasLimit)
                .toBigDecimal().movePointLeft(cryptoCurrency.decimals),
        ),
    )
    else -> this
}

/**
 * Increase gasLimit for Fee.Ethereum
 */
fun Fee.increaseGasLimitBy(percent: BigInteger): Fee {
    if (this !is Fee.Ethereum) return this
    val gasLimit = this.gasLimit
    val increasedGasPrice = this.amount.value?.movePointRight(this.amount.decimals)
        ?.divide(gasLimit.toBigDecimal(), RoundingMode.HALF_UP)
    val increasedGasLimit = gasLimit
        .multiply(percent)
        .divide(HUNDRED_PERCENT)
    val increasedAmount = this.amount.copy(
        value = increasedGasLimit.toBigDecimal().multiply(increasedGasPrice).movePointLeft(this.amount.decimals),
    )
    return when (this) {
        is Fee.Ethereum.EIP1559 -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
        is Fee.Ethereum.Legacy -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
    }
}