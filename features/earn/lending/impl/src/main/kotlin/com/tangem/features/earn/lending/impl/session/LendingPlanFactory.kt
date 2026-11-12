package com.tangem.features.earn.lending.impl.session

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.earn.lending.impl.product.LendingTexts
import com.tangem.features.earn.lending.impl.provider.LendingProvider
import com.tangem.features.earn.lending.impl.provider.LendingTransactions
import com.tangem.features.earn.lending.impl.provider.LendingTxError
import com.tangem.features.earn.lending.impl.provider.LendingTxExpectation
import com.tangem.features.earn.lending.impl.supplier.LendingAssetState
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.EarnSessionError
import com.tangem.features.earn.product.api.session.WithdrawMode
import java.math.BigDecimal
import javax.inject.Inject

internal class LendingPlanFactory @Inject constructor(
    private val transactions: LendingTransactions,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val lendingProvider: LendingProvider,
) {

    suspend fun feeLevels(
        userWalletId: UserWalletId,
        state: LendingAssetState,
        operation: LendingOperation,
        amount: BigDecimal,
    ): Either<GetFeeError, TransactionFee> = either {
        val userWallet = getUserWalletUseCase(userWalletId).getOrElse { raise(GetFeeError.UnknownError) }
        val txs = build(userWalletId = userWalletId, state = state, operation = operation, amount = amount)
            .getOrElse { raise(GetFeeError.UnknownError) }
        transactions.feeLevels(userWallet, state.asset, txs).bind()
    }

    suspend fun plan(
        userWalletId: UserWalletId,
        state: LendingAssetState,
        action: EarnFlowAction,
        amount: BigDecimal,
        fee: Fee,
    ): Either<EarnSessionError, LendingPlan> = either {
        val operation = ensureNotNull(action.toLendingOperation()) { EarnSessionError.Failed() }
        val userWallet = getUserWalletUseCase(userWalletId).getOrElse { raise(EarnSessionError.Failed()) }
        val providerState = ensureNotNull(state.market.providerState) { EarnSessionError.Failed() }
        val txs = build(userWalletId = userWalletId, state = state, operation = operation, amount = amount)
            .mapLeft(::toSessionError)
            .bind()
        val estimated = transactions.estimate(userWallet, state.asset, txs).mapLeft(EarnSessionError::Fee).bind()
        val plannedOperation = when (operation) {
            LendingOperation.Deposit -> LendingPlannedOperation.Deposit
            LendingOperation.Withdraw -> {
                val position = ensureNotNull(state.market.position) { EarnSessionError.Failed() }
                LendingPlannedOperation.Withdraw(isEntire = isEntireWithdrawal(amount, position.cryptoAmount))
            }
        }
        LendingPlan(
            fee = fee,
            expectedAmount = amount,
            rows = emptyList(),
            withdrawMode = WithdrawMode.Instant.takeIf { operation == LendingOperation.Withdraw },
            action = action,
            operation = plannedOperation,
            amount = amount,
            txs = estimated,
            planKey = providerState.planKey,
        )
    }

    /**
     * The expectation comes from the plan: the position keeps growing with interest, so recomputing whether a
     * withdrawal is entire from a fresher state would reject the transaction the user confirmed
     */
    suspend fun verify(
        userWalletId: UserWalletId,
        state: LendingAssetState,
        plan: LendingPlan,
    ): Either<LendingTxError, Unit> = either {
        val providerState = ensureNotNull(state.market.providerState) { LendingTxError.ReadFailed }
        // The provider encodes this address as the module owner, so the check must use the same one
        val owner = ensureNotNull(state.address) { LendingTxError.ReadFailed }
        val expected = when (val operation = plan.operation) {
            LendingPlannedOperation.Deposit -> LendingTxExpectation.Deposit(owner = owner, amount = plan.amount)
            is LendingPlannedOperation.Withdraw -> LendingTxExpectation.Withdraw(
                owner = owner,
                amount = plan.amount,
                isEntire = operation.isEntire,
            )
        }
        transactions.verify(
            userWalletId = userWalletId,
            asset = state.asset,
            txs = plan.txs,
            expected = expected,
            state = providerState,
        ).bind()
    }

    private suspend fun build(
        userWalletId: UserWalletId,
        state: LendingAssetState,
        operation: LendingOperation,
        amount: BigDecimal,
    ): Either<LendingTxError, List<TransactionData.Uncompiled>> = either {
        val providerState = ensureNotNull(state.market.providerState) { LendingTxError.ReadFailed }
        when (operation) {
            LendingOperation.Deposit -> transactions.deposit(
                userWalletId = userWalletId,
                asset = state.asset,
                amount = amount,
                state = providerState,
            ).bind()
            LendingOperation.Withdraw -> {
                val position = ensureNotNull(state.market.position) { LendingTxError.ReadFailed }
                val withdrawal = transactions.withdraw(
                    userWalletId = userWalletId,
                    asset = state.asset,
                    amount = amount,
                    position = position.cryptoAmount,
                    state = providerState,
                ).bind()
                listOf(withdrawal)
            }
        }
    }

    private fun isEntireWithdrawal(amount: BigDecimal, position: BigDecimal): Boolean = amount >= position

    private fun toSessionError(error: LendingTxError): EarnSessionError = when (error) {
        LendingTxError.UpdateRequired -> EarnSessionError.Rejected(LendingTexts.updateRequired(lendingProvider.info))
        LendingTxError.ReadFailed, is LendingTxError.Unverified -> EarnSessionError.Failed()
    }
}