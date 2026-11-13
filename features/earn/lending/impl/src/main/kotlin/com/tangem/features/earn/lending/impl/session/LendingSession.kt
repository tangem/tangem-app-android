package com.tangem.features.earn.lending.impl.session

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.models.YieldSupplyTransactionOutcome
import com.tangem.features.earn.lending.impl.product.LendingEarnProduct
import com.tangem.features.earn.lending.impl.product.LendingTexts
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
import kotlin.time.Duration.Companion.seconds

internal class LendingSession @AssistedInject constructor(
    @Assisted private val params: EarnProduct.Params,
    private val lendingSupplier: LendingSupplier,
    private val lendingProvider: LendingProvider,
    private val planFactory: LendingPlanFactory,
    private val sender: LendingTxSender,
    private val transactionRepository: YieldSupplyTransactionRepository,
) : EarnSession {

    override suspend fun loadFee(action: EarnFlowAction, amount: EarnAmount?): Either<GetFeeError, TransactionFee> =
        either {
            val operation = ensureNotNull(action.toLendingOperation()) { GetFeeError.UnknownError }
            val state = ensureNotNull(currentState()) { GetFeeError.UnknownError }
            val value = ensureNotNull(amount.resolve(state, operation)) { GetFeeError.UnknownError }
            planFactory.feeLevels(
                userWalletId = params.userWalletId,
                state = state,
                operation = operation,
                amount = value,
            ).bind()
        }

    override suspend fun loadFeeExtended(
        action: EarnFlowAction,
        amount: EarnAmount?,
        feeCurrency: CryptoCurrencyStatus?,
    ): Either<GetFeeError, TransactionFeeExtended> = loadFee(action, amount).map { fee ->
        TransactionFeeExtended(transactionFee = fee, feeTokenId = params.currency.id)
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
        if (operation == LendingOperation.Deposit) ensureAboveMinDeposit(state = state, amount = value)
        planFactory.plan(
            userWalletId = params.userWalletId,
            state = state,
            action = action,
            amount = value,
            fee = fee.fee,
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
        val selectedFee = ensureNotNull(plan.fee as? Fee.Ethereum) { EarnSessionError.Failed() }
        val sentGasLimits = ensureNotNull(plan.sentGasLimits()) { EarnSessionError.Failed() }
        val state = ensureNotNull(currentState()) { EarnSessionError.Failed() }
        ensure(!state.hasPendingTx) { EarnSessionError.Rejected(LendingTexts.pendingTransaction) }
        ensureAffordable(plan = plan, operation = operation, state = state, fee = selectedFee)

        val newPlan = planFactory.plan(
            userWalletId = params.userWalletId,
            state = state,
            action = plan.action,
            amount = plan.amount,
            fee = plan.fee,
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
            ensure(!plan.isFeeBelowEstimate) { EarnSessionError.Rejected(LendingTexts.gasLimitTooLow) }
            return@either EarnVerification.Changed(newPlan)
        }

        planFactory.verify(userWalletId = params.userWalletId, state = state, plan = plan)
            .mapLeft { EarnSessionError.Failed() }
            .bind()
        EarnVerification.Verified(LendingVerifiedPlan(plan))
    }

    private fun Raise<EarnSessionError>.ensureAffordable(
        plan: LendingPlan,
        operation: LendingOperation,
        state: LendingAssetState,
        fee: Fee,
    ) {
        val balance = ensureNotNull(state.balance) { EarnSessionError.Failed() }
        val feeValue = ensureNotNull(fee.amount.value) { EarnSessionError.Failed() }
        when (operation) {
            LendingOperation.Deposit -> {
                ensure(state.market.isOpen) { EarnSessionError.Rejected(LendingTexts.depositsPaused) }
                ensure(plan.amount + feeValue <= balance) { EarnSessionError.InsufficientBalance }
            }
            LendingOperation.Withdraw -> {
                val maxWithdraw = ensureNotNull(state.market.maxWithdraw) { EarnSessionError.Failed() }
                ensure(plan.amount <= maxWithdraw) { EarnSessionError.ExceedsLimit(maxWithdraw) }
                // The withdrawn ETH arrives only with the transaction, so its gas is paid from the wallet balance
                ensure(feeValue <= balance) { EarnSessionError.InsufficientBalance }
            }
        }
    }

    /** A minimum the provider could not compute does not block the deposit, the same as on the product screen */
    private suspend fun Raise<EarnSessionError>.ensureAboveMinDeposit(state: LendingAssetState, amount: BigDecimal) {
        val minDeposit = lendingProvider.minDeposit(params.userWalletId, state.asset).getOrNull() ?: return
        ensure(amount >= minDeposit) { EarnSessionError.BelowMinimum(minDeposit) }
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
            refreshProvider()
            emit(EarnSigningStep.Failed(SendTransactionError.UnknownError()))
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
        refreshProvider()
        val result = when (operation) {
            LendingOperation.Deposit -> lendingDepositResult(plan.amount, params.currency, lendingProvider.info.title)
            LendingOperation.Withdraw -> lendingWithdrawResult(plan.amount, params.currency, plan.fee)
        }
        emit(EarnSigningStep.Done(ref, result))
    }

    /** The next plan must see what the sent transactions changed, such as a module that is already deployed */
    private suspend fun refreshProvider() {
        val asset = lendingProvider.resolveAsset(params.currency) ?: return
        lendingProvider.refresh(params.userWalletId, asset)
    }

    private suspend fun currentState(): LendingAssetState? =
        lendingSupplier(params.userWalletId).first().assets[params.currency.id]

    private fun LendingAssetState.maxAmount(operation: LendingOperation): BigDecimal? = when (operation) {
        LendingOperation.Deposit -> balance
        LendingOperation.Withdraw -> market.maxWithdraw
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

/** File-private so that only [LendingSession.verify] creates it and [LendingSession.execute] sends nothing else */
private class LendingVerifiedPlan(override val plan: LendingPlan) : VerifiedPlan