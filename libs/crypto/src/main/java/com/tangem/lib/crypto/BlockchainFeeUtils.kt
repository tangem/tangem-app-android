package com.tangem.lib.crypto

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import java.math.BigInteger
import java.math.RoundingMode

/**
 * !!!IMPORTANT!!!
 * Methods for tuning transaction fee of different blockchains
 *
 * Temporary solution for domain specific logic for Blockchain.
 * Instead of creating repositories and unnecessary and overkill use cases
 */
object BlockchainFeeUtils {

    private val HUNDRED_PERCENT = BigInteger("100")

    /**
     * We need to increase gasLimit for Ethereum fees for 2 cases
     *
     * DEX: for dex calculated gasLimit for given data might be changed when transaction processing
     * for that case dex providers recommend to increase gasLimit for few percents to ensure transaction completes
     *
     * CEX: for that case we calculate fee for random generated address and gasLimit might be different for it
     * and result address to send. That's why we should increase gasLimit a little
     *
     */
    fun TransactionFee.patchTransactionFeeForSwap(increaseBy: Int): TransactionFee {
        return when (this) {
            is TransactionFee.Choosable -> {
                this.copy(
                    minimum = this.minimum.increaseEthGasLimitInNeeded(increaseBy),
                    normal = this.normal.increaseEthGasLimitInNeeded(increaseBy),
                    priority = this.priority.increaseEthGasLimitInNeeded(increaseBy),
                )
            }
            is TransactionFee.Single -> this.copy(normal = this.normal.increaseEthGasLimitInNeeded(increaseBy))
        }
    }

    private fun Fee.increaseEthGasLimitInNeeded(increaseBy: Int): Fee {
        return when (this) {
            is Fee.Ethereum.EIP1559,
            is Fee.Ethereum.Legacy,
            -> this.increaseGasLimitBy(increaseBy)
            is Fee.Alephium,
            is Fee.Aptos,
            is Fee.Bitcoin,
            is Fee.CardanoToken,
            is Fee.Common,
            is Fee.Filecoin,
            is Fee.Hedera,
            is Fee.Kaspa,
            is Fee.Sui,
            is Fee.Tron,
            is Fee.VeChain,
            -> this
        }
    }

    /**
     * Increase gasLimit for Fee.Ethereum
     */
    private fun Fee.increaseGasLimitBy(percentage: Int): Fee {
        if (this !is Fee.Ethereum) return this
        val gasLimit = this.gasLimit
        val increasedGasPrice = this.amount.value?.movePointRight(this.amount.decimals)
            ?.divide(gasLimit.toBigDecimal(), RoundingMode.HALF_UP)
        val increasedGasLimit = gasLimit
            .multiply(percentage.toBigInteger())
            .divide(HUNDRED_PERCENT)
        val increasedAmount = this.amount.copy(
            value = increasedGasLimit.toBigDecimal().multiply(increasedGasPrice).movePointLeft(this.amount.decimals),
        )
        return when (this) {
            is Fee.Ethereum.EIP1559 -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
            is Fee.Ethereum.Legacy -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
        }
    }
}