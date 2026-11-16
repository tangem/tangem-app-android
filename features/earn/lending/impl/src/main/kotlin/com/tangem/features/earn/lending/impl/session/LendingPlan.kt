package com.tangem.features.earn.lending.impl.session

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.earn.lending.impl.provider.isSameTransactionAs
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.EarnPlan
import com.tangem.features.earn.product.api.session.Withdraw
import com.tangem.features.earn.product.api.session.WithdrawMode
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.Duration

/**
 * @property fee       the fee picked in the selector, priced for the summed gas limit of [txs]
 * @property feeType   which selector option [fee] is, reported to analytics
 * @property operation what [txs] were built to do; they are verified against it, not against a fresher state
 * @property txs       estimated transactions, the preparation (if any) first
 * @property planKey   the provider state the transactions were built for
 */
internal data class LendingPlan(
    override val fee: Fee,
    val feeType: AnalyticsParam.FeeType,
    override val expectedAmount: BigDecimal?,
    override val rows: List<EarnRow>,
    override val withdrawMode: WithdrawMode?,
    val action: EarnFlowAction,
    val operation: LendingPlannedOperation,
    val amount: BigDecimal,
    val txs: List<TransactionData.Uncompiled>,
    val planKey: String,
    val isWithUpgrade: Boolean,
) : EarnPlan {

    override val quoteTtl: Duration? = null

    override val notice: TextReference? = null

    override val isEstimate: Boolean = false

    val totalGasLimit: BigInteger = txs.fold(BigInteger.ZERO) { total, tx -> total + (tx.gasLimit ?: BigInteger.ZERO) }

    /** The selected fee was set below the estimate of [txs], which only a custom gas limit does */
    val isFeeBelowEstimate: Boolean
        get() = (fee as? Fee.Ethereum)?.let { it.gasLimit < totalGasLimit } == true

    /**
     * The gas limit each of [txs] is sent with: the selector priced their sum, so each preparation keeps its estimate
     * and the last transaction gets the rest, which also gives it a raised custom limit
     *
     * @return null when a fee is not an Ethereum one
     */
    fun sentGasLimits(): List<BigInteger>? {
        val selected = fee as? Fee.Ethereum ?: return null
        if (txs.isEmpty()) return null
        val preparationLimits = txs.dropLast(1).map { tx -> tx.gasLimit ?: return null }
        val preparationGas = preparationLimits.fold(BigInteger.ZERO, BigInteger::add)
        return preparationLimits + (selected.gasLimit - preparationGas)
    }

    fun hasSameTransactionsAs(other: LendingPlan, networkId: String): Boolean = txs.size == other.txs.size &&
        txs.zip(other.txs).all { (tx, otherTx) -> tx.isSameTransactionAs(otherTx, networkId) }
}

internal val TransactionData.Uncompiled.gasLimit: BigInteger?
    get() = (fee as? Fee.Ethereum)?.gasLimit

internal sealed interface LendingPlannedOperation {

    data object Deposit : LendingPlannedOperation

    /** @property isEntire the whole position is withdrawn, which also deactivates the coin in the provider */
    data class Withdraw(val isEntire: Boolean) : LendingPlannedOperation
}

internal enum class LendingOperation { Deposit, Withdraw }

internal fun EarnFlowAction.toLendingOperation(): LendingOperation? = when (this) {
    is Deposit -> LendingOperation.Deposit
    is Withdraw -> LendingOperation.Withdraw
    else -> null
}