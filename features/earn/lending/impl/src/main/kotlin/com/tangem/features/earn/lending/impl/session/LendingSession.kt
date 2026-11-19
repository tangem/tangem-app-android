package com.tangem.features.earn.lending.impl.session

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.models.YieldSupplyTransactionOutcome
import com.tangem.features.earn.lending.impl.analytics.LendingAnalyticsSender
import com.tangem.features.earn.lending.impl.product.LendingEarnProduct
import com.tangem.features.earn.lending.impl.product.LendingStateMapper
import com.tangem.features.earn.lending.impl.provider.LendingProvider
import com.tangem.features.earn.lending.impl.supplier.LendingAssetState
import com.tangem.features.earn.lending.impl.supplier.LendingSupplier
import com.tangem.features.earn.product.api.analytics.EarnTxAnalytics
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.session.EarnAmount
import com.tangem.features.earn.product.api.session.EarnFeeSelection
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.EarnPlan
import com.tangem.features.earn.product.api.session.EarnSession
import com.tangem.features.earn.product.api.session.EarnSessionError
import com.tangem.features.earn.product.api.session.EarnSigningStep
import com.tangem.features.earn.product.api.session.EarnTxRef
import com.tangem.features.earn.product.api.session.EarnTxStatus
import com.tangem.features.earn.product.api.session.EarnVerification
import com.tangem.features.earn.product.api.session.VerifiedPlan
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
internal class LendingSession @AssistedInject constructor(
    @Assisted private val params: EarnProduct.Params,
    private val lendingSupplier: LendingSupplier,
    private val lendingProvider: LendingProvider,
    private val planFactory: LendingPlanFactory,
    private val sender: LendingTxSender,
    private val transactionRepository: YieldSupplyTransactionRepository,
    private val analyticsSender: LendingAnalyticsSender,
    private val accountNames: LendingAccountNames,
    private val gaslessFees: LendingGaslessFees,
) : EarnSession {

    override suspend fun loadFee(action: EarnFlowAction, amount: EarnAmount?): Either<GetFeeError, TransactionFee> =
        either {
            val input = ensureNotNull(feeInput(action, amount)) { GetFeeError.UnknownError }
            planFactory.feeLevels(
                userWalletId = params.userWalletId,
                state = input.state,
                operation = input.operation,
                amount = input.amount,
            ).bind()
        }

    override suspend fun loadFeeExtended(
        action: EarnFlowAction,
        amount: EarnAmount?,
        feeCurrency: CryptoCurrencyStatus?,
    ): Either<GetFeeError, TransactionFeeExtended> = either {
        val input = ensureNotNull(feeInput(action, amount)) { GetFeeError.UnknownError }
        planFactory.feeExtended(
            userWalletId = params.userWalletId,
            state = input.state,
            operation = input.operation,
            amount = input.amount,
            isGaslessAllowed = action.isGaslessAllowed,
            feeToken = feeCurrency,
        ).bind()
    }

    override suspend fun maxAmount(action: EarnFlowAction): Either<EarnSessionError, BigDecimal> = either {
        val operation = ensureNotNull(action.toLendingOperation()) { EarnSessionError.Failed() }
        val state = ensureNotNull(currentState()) { EarnSessionError.Failed() }
        ensureNotNull(state.maxAmount(operation)) { EarnSessionError.Failed() }
    }

    override suspend fun prepare(
        action: EarnFlowAction,
        amount: EarnAmount?,
        fee: EarnFeeSelection,
    ): Either<EarnSessionError, EarnPlan> = either {
        val operation = ensureNotNull(action.toLendingOperation()) { EarnSessionError.Failed() }
        val state = ensureNotNull(currentState()) { EarnSessionError.Failed() }
        val value = ensureNotNull(amount.resolve(state, operation)) { EarnSessionError.Failed() }
        ensure(value.signum() > 0) { EarnSessionError.Failed() }
        planFactory.plan(
            userWalletId = params.userWalletId,
            state = state,
            action = action,
            amount = value,
            fee = fee,
        ).bind()
    }

    override suspend fun verify(plan: EarnPlan): EarnVerification {
        val lendingPlan = plan as? LendingPlan ?: return EarnVerification.Rejected(EarnSessionError.Failed())
        return recheck(lendingPlan).fold(ifLeft = EarnVerification::Rejected, ifRight = { it })
    }

    override fun execute(plan: VerifiedPlan): Flow<EarnSigningStep> = flow {
        val lendingPlan = (plan as? LendingVerifiedPlan)?.plan
        val operation = lendingPlan?.action?.toLendingOperation()
        if (lendingPlan == null || operation == null) {
            emit(EarnSigningStep.Failed(SendTransactionError.UnknownError()))
            return@flow
        }

        val analytics = lendingTxAnalytics(lendingPlan.action)
        emit(EarnSigningStep.AwaitingConfirmation(stepLabel = null, tx = analytics))
        sender.send(params.userWalletId, params.currency, lendingPlan).fold(
            ifLeft = { error ->
                analyticsSender.onSendFailed(plan = lendingPlan, operation = operation, error = error)
                refreshProvider()
                emit(EarnSigningStep.Failed(error))
            },
            ifRight = { hashes ->
                emitSent(hashes = hashes, plan = lendingPlan, operation = operation, analytics = analytics)
            },
        )
    }

    /** Gives up after [MAX_TRACK_ATTEMPTS] polls, leaving the transaction unconfirmed */
    override fun track(ref: EarnTxRef): Flow<EarnTxStatus> = flow {
        val asset = lendingProvider.resolveAsset(params.currency)
        val wrappedToken = asset?.wrappedToken
        if (asset?.currency !is CryptoCurrency.Coin || wrappedToken == null) {
            emit(EarnTxStatus(TxInfo.TransactionStatus.Unconfirmed))
            return@flow
        }

        repeat(MAX_TRACK_ATTEMPTS) { attempt ->
            val outcome = transactionRepository.getTransactionOutcome(ref.userWalletId, wrappedToken, ref.id)
            val status = outcome.toTxStatus()
            emit(EarnTxStatus(status))
            if (status != TxInfo.TransactionStatus.Unconfirmed) return@flow
            if (attempt < MAX_TRACK_ATTEMPTS - 1) delay(POLL_INTERVAL)
        }
    }.distinctUntilChanged()

    private suspend fun recheck(plan: LendingPlan): Either<EarnSessionError, EarnVerification> = either {
        val operation = ensureNotNull(plan.action.toLendingOperation()) { EarnSessionError.Failed() }
        val sentGasLimits = ensureNotNull(plan.sentGasLimits()) { EarnSessionError.Failed() }
        val state = ensureNotNull(currentState()) { EarnSessionError.Failed() }
        ensure(!state.hasPendingTx) { EarnSessionError.Rejected(LendingStateMapper.PENDING_BLOCK.reason) }
        val gaslessFee = plan.gaslessFee
        val feePayingBalance = if (gaslessFee != null) {
            gaslessFees.balanceOf(userWalletId = params.userWalletId, fee = gaslessFee)
        } else {
            state.balance
        }
        ensureAffordable(plan = plan, operation = operation, state = state, feePayingBalance = feePayingBalance)

        val newPlan = planFactory.plan(
            userWalletId = params.userWalletId,
            state = state,
            action = plan.action,
            amount = plan.rebuildAmount(state),
            fee = EarnFeeSelection(fee = plan.fee, extended = plan.gaslessFee, feeType = plan.feeType),
        ).bind()
        val isRebuilt = newPlan.planKey != plan.planKey ||
            !newPlan.hasSameTransactionsAs(plan, params.currency.network.rawId)
        if (isRebuilt) return@either EarnVerification.Changed(newPlan)

        val canRunOutOfGas = newPlan.txs.zip(sentGasLimits).any { (tx, sentGasLimit) ->
            val freshGasLimit = tx.gasLimit ?: return@any true
            freshGasLimit > sentGasLimit
        }
        if (canRunOutOfGas) {
            // The flow keeps a custom fee across its reload, so a plan rebuilt for it would come back here again
            ensure(!plan.isFeeBelowEstimate) { gasLimitError(operation, required = newPlan.totalGasLimit) }
            return@either EarnVerification.Changed(newPlan)
        }

        planFactory.verify(userWalletId = params.userWalletId, state = state, plan = plan)
            .mapLeft { EarnSessionError.Failed() }
            .bind()
        EarnVerification.Verified(LendingVerifiedPlan(plan))
    }

    /** The text speaks of a deposit and names the limit it needs: a withdrawal has no such text and just fails */
    private fun gasLimitError(operation: LendingOperation, required: BigInteger): EarnSessionError = when (operation) {
        LendingOperation.Deposit -> EarnSessionError.Rejected(
            resourceReference(R.string.lending_error_gas_limit_too_low, wrappedList(required.toString())),
        )
        LendingOperation.Withdraw -> EarnSessionError.Failed()
    }

    private fun Raise<EarnSessionError>.ensureAffordable(
        plan: LendingPlan,
        operation: LendingOperation,
        state: LendingAssetState,
        feePayingBalance: BigDecimal?,
    ) {
        val feeValue = ensureNotNull(plan.fee.amount.value) { EarnSessionError.Failed() }
        when (operation) {
            LendingOperation.Deposit -> {
                val balance = ensureNotNull(state.balance) { EarnSessionError.Failed() }
                ensure(state.market.isOpen) { EarnSessionError.Rejected(LendingStateMapper.CLOSED_BLOCK.reason) }
                ensure(plan.amount + feeValue <= balance) { EarnSessionError.InsufficientBalance }
            }
            LendingOperation.Withdraw -> {
                val maxWithdraw = ensureNotNull(state.market.maxWithdraw) { EarnSessionError.Failed() }
                ensure(plan.amount <= maxWithdraw) { EarnSessionError.ExceedsLimit(maxWithdraw) }
                // The withdrawn ETH arrives only with the transaction, so the fee is paid from what the wallet holds
                val balance = ensureNotNull(feePayingBalance) { EarnSessionError.Failed() }
                ensure(feeValue <= balance) { EarnSessionError.InsufficientBalance }
            }
        }
    }

    /** The plan's result follows the last transaction: the deposit or withdrawal, not the preparation */
    private suspend fun FlowCollector<EarnSigningStep>.emitSent(
        hashes: List<String>,
        plan: LendingPlan,
        operation: LendingOperation,
        analytics: EarnTxAnalytics,
    ) {
        val hash = hashes.lastOrNull()
        if (hash == null) {
            val error = SendTransactionError.UnknownError()
            analyticsSender.onSendFailed(plan = plan, operation = operation, error = error)
            refreshProvider()
            emit(EarnSigningStep.Failed(error))
            return
        }

        val ref = EarnTxRef(
            version = TX_REF_VERSION,
            productId = LendingEarnProduct.ID,
            id = hash,
            userWalletId = params.userWalletId,
            currencyId = params.currency.id,
            createdAt = Clock.System.now(),
            analytics = analytics,
        )
        // Sent goes first: once the flow knows the hash, it never offers to send the plan again
        emit(EarnSigningStep.Sent(ref))
        analyticsSender.onSent(plan = plan, operation = operation, currency = params.currency)
        refreshProvider()
        val result = when (operation) {
            LendingOperation.Deposit -> lendingDepositResult(
                amount = plan.amount,
                currency = params.currency,
                accountName = accountNames.nameOf(params.userWalletId, params.currency.id),
            )
            LendingOperation.Withdraw -> lendingWithdrawResult(plan.amount, params.currency, plan.fee)
        }
        emit(EarnSigningStep.Done(ref, result))
    }

    /** The next plan must see what the sent transactions changed, such as a module that is already deployed */
    private suspend fun refreshProvider() {
        val asset = lendingProvider.resolveAsset(params.currency) ?: return
        lendingProvider.refresh(params.userWalletId, asset)
    }

    private suspend fun feeInput(action: EarnFlowAction, amount: EarnAmount?): FeeInput? {
        val operation = action.toLendingOperation() ?: return null
        val state = currentState() ?: return null
        val value = amount.resolve(state, operation) ?: return null
        return FeeInput(operation = operation, state = state, amount = value)
    }

    private suspend fun currentState(): LendingAssetState? =
        lendingSupplier(params.userWalletId).first().assets[params.currency.id]

    private fun LendingAssetState.maxAmount(operation: LendingOperation): BigDecimal? = when (operation) {
        LendingOperation.Deposit -> balance
        LendingOperation.Withdraw -> market.maxWithdraw
    }

    /**
     * An entire withdrawal is rebuilt for the fresh position: interest grows it after preparation, and the planned
     * amount would fall short of it and turn the exit into a partial withdrawal that leaves dust behind
     */
    private fun LendingPlan.rebuildAmount(state: LendingAssetState): BigDecimal {
        val isEntire = (operation as? LendingPlannedOperation.Withdraw)?.isEntire == true
        return if (isEntire) state.maxAmount(LendingOperation.Withdraw) ?: amount else amount
    }

    /** The flow passes exact amounts; without one the whole source is meant */
    private fun EarnAmount?.resolve(state: LendingAssetState, operation: LendingOperation): BigDecimal? = when (this) {
        is EarnAmount.Exact -> value
        EarnAmount.Entire, null -> state.maxAmount(operation)
    }

    private fun YieldSupplyTransactionOutcome?.toTxStatus(): TxInfo.TransactionStatus = when (this) {
        null -> TxInfo.TransactionStatus.Unconfirmed
        YieldSupplyTransactionOutcome.Confirmed -> TxInfo.TransactionStatus.Confirmed
        YieldSupplyTransactionOutcome.Reverted -> TxInfo.TransactionStatus.Failed
    }

    @AssistedFactory
    interface Factory : EarnSession.Factory {
        override fun create(params: EarnProduct.Params): LendingSession
    }

    private companion object {
        const val TX_REF_VERSION = 1
        const val MAX_TRACK_ATTEMPTS = 15
        val POLL_INTERVAL = 10.seconds
    }
}

private class FeeInput(val operation: LendingOperation, val state: LendingAssetState, val amount: BigDecimal)

/** File-private so that only [LendingSession.verify] creates it and [LendingSession.execute] sends nothing else */
private class LendingVerifiedPlan(override val plan: LendingPlan) : VerifiedPlan