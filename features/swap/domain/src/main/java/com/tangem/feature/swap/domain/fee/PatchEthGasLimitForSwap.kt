package com.tangem.feature.swap.domain.fee

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Increases the Ethereum gas limit on a [com.tangem.blockchain.common.transaction.TransactionFee] by the configured [percentage].
 *
 * [REDACTED_TASK_KEY] — extracted from `SwapInteractorImpl.patchTransactionFeeForSwap` so the bump rule
 * becomes a first-class, mockable, swappable use case. Two singletons are wired via DI in the
 * swap module with custom `@Qualifier` annotations:
 *  - `@SwapDexGasLimit` → [DEX_PERCENTAGE] (12% bump for DEX swap fees)
 *  - `@SwapSendGasLimit` → [SEND_PERCENTAGE] (5% bump for CEX/send fees)
 *
 * Behavior is byte-for-byte identical to the original private helpers in `SwapInteractorImpl`:
 *  - [com.tangem.blockchain.common.transaction.Fee.Ethereum.Legacy] / [com.tangem.blockchain.common.transaction.Fee.Ethereum.EIP1559]: gasLimit *= percentage / 100, amount
 *    recomputed = (newGasLimit * gasPrice) shifted left by amount decimals; decimals preserved.
 *  - [com.tangem.blockchain.common.transaction.Fee.Ethereum.TokenCurrency]: throws `IllegalStateException("handle in [REDACTED_TASK_KEY]")`.
 *  - All other [com.tangem.blockchain.common.transaction.Fee] subtypes (Common, Bitcoin, Tron, etc.): returned unchanged.
 */
class PatchEthGasLimitForSwap(private val percentage: Int) {

    operator fun invoke(transactionFee: TransactionFee): TransactionFee {
        return when (transactionFee) {
            is TransactionFee.Choosable -> transactionFee.copy(
                minimum = transactionFee.minimum.increaseEthGasLimitInNeeded(percentage),
                normal = transactionFee.normal.increaseEthGasLimitInNeeded(percentage),
                priority = transactionFee.priority.increaseEthGasLimitInNeeded(percentage),
            )
            is TransactionFee.Single -> transactionFee.copy(
                normal = transactionFee.normal.increaseEthGasLimitInNeeded(percentage),
            )
        }
    }

    private fun Fee.increaseEthGasLimitInNeeded(increaseBy: Int): Fee {
        return when (this) {
            is Fee.Ethereum.TokenCurrency -> error("handle in [REDACTED_TASK_KEY]")
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

    private fun Fee.increaseGasLimitBy(percentage: Int): Fee {
        if (this !is Fee.Ethereum) return this
        val gasLimit = this.gasLimit
        val increasedGasPrice = this.amount.value?.movePointRight(this.amount.decimals)
            ?.divide(gasLimit.toBigDecimal(), RoundingMode.HALF_UP)
        val increasedGasLimit = gasLimit.multiply(percentage.toBigInteger()).divide(HUNDRED_PERCENT)
        val increasedAmount = this.amount.copy(
            value = increasedGasLimit.toBigDecimal().multiply(increasedGasPrice).movePointLeft(this.amount.decimals),
        )
        return when (this) {
            is Fee.Ethereum.EIP1559 -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
            is Fee.Ethereum.Legacy -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
            is Fee.Ethereum.TokenCurrency -> error("handle in [REDACTED_TASK_KEY]")
        }
    }

    companion object {
        /** 12% bump used by DEX provider fee patching. */
        const val DEX_PERCENTAGE = 112

        /** 5% bump used by CEX/send fee patching. */
        const val SEND_PERCENTAGE = 105

        private val HUNDRED_PERCENT = BigInteger("100")
    }
}