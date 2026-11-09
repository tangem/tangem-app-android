package com.tangem.features.earn.impl.fake

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnAmount
import com.tangem.features.earn.product.api.session.EarnFeeSelection
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.EarnPlan
import com.tangem.features.earn.product.api.session.EarnSession
import com.tangem.features.earn.product.api.session.EarnSessionError
import com.tangem.features.earn.product.api.session.EarnSigningStep
import com.tangem.features.earn.product.api.session.EarnStatusDetail
import com.tangem.features.earn.product.api.session.EarnTxRef
import com.tangem.features.earn.product.api.session.EarnTxStatus
import com.tangem.features.earn.product.api.session.EarnVerification
import com.tangem.features.earn.product.api.session.VerifiedPlan
import com.tangem.features.earn.product.api.session.Withdraw
import com.tangem.features.earn.product.api.session.WithdrawMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.math.BigDecimal

internal class FakeEarnSession(
    private val preset: FakeEarnSessionPreset,
    private val behavior: FakeEarnSessionBehavior,
) : EarnSession {

    override suspend fun loadFee(action: EarnFlowAction, amount: EarnAmount?): Either<GetFeeError, TransactionFee> =
        if (action.isSupported()) {
            TransactionFee.Single(normal = fakeFee(FAKE_FEE_ETH)).right()
        } else {
            GetFeeError.UnknownError.left()
        }

    override suspend fun loadFeeExtended(
        action: EarnFlowAction,
        amount: EarnAmount?,
        feeCurrency: CryptoCurrencyStatus?,
    ): Either<GetFeeError, TransactionFeeExtended> = GetFeeError.UnknownError.left()

    override suspend fun maxAmount(action: EarnFlowAction): Either<EarnSessionError, BigDecimal> = when (action) {
        is Deposit -> FAKE_WALLET_BALANCE.right()
        is Withdraw -> FAKE_POSITION.right()
        else -> EarnSessionError.Failed().left()
    }

    override suspend fun prepare(
        action: EarnFlowAction,
        amount: EarnAmount?,
        fee: EarnFeeSelection,
    ): Either<EarnSessionError, EarnPlan> {
        val plan = when (action) {
            is Deposit -> amount.resolve(entire = FAKE_WALLET_BALANCE)?.let { depositPlan(it, fee) }
            is Withdraw -> amount.resolve(entire = FAKE_POSITION)?.let { withdrawPlan(it, fee) }
            is FakeClaim -> claimPlan(fee).takeIf { amount == null }
            else -> null
        }
        return plan?.right() ?: EarnSessionError.Failed().left()
    }

    override suspend fun verify(plan: EarnPlan): EarnVerification = when {
        plan !is FakeDepositPlan -> EarnVerification.Rejected(EarnSessionError.Failed())
        behavior.isFeeGrowingOnVerify && !plan.isFeeRefreshed ->
            EarnVerification.Changed(plan.copy(fee = fakeFee(FAKE_FEE_GROWN_ETH), isFeeRefreshed = true))
        else -> EarnVerification.Verified(FakeVerifiedPlan(plan))
    }

    override fun execute(plan: VerifiedPlan): Flow<EarnSigningStep> = flow {
        val fakePlan = plan.plan as? FakeDepositPlan
        if (fakePlan == null) {
            emit(EarnSigningStep.Failed(SendTransactionError.UnknownError()))
            return@flow
        }

        emit(EarnSigningStep.AwaitingConfirmation(stepLabel = null, tx = fakeTxAnalytics(fakePlan.actionId)))
        if (behavior.isExecuteFailing) {
            emit(EarnSigningStep.Failed(SendTransactionError.NetworkError(message = "fake", code = null)))
            return@flow
        }

        val ref = fakeTxRef(preset, fakePlan.actionId)
        emit(EarnSigningStep.Sent(ref))
        val result = if (fakePlan.actionId == Withdraw.ID) {
            fakeWithdrawResult(title = resultTitle(fakePlan), period = withdrawPeriod())
        } else {
            fakeResult(title = resultTitle(fakePlan), provider = providerName())
        }
        emit(EarnSigningStep.Done(ref, result))
    }

    override fun track(ref: EarnTxRef): Flow<EarnTxStatus> {
        val isQueuedWithdraw = preset == FakeEarnSessionPreset.Vault && ref.id == fakeTxRef(preset, Withdraw.ID).id
        return if (isQueuedWithdraw) {
            flowOf(
                EarnTxStatus(TxInfo.TransactionStatus.Unconfirmed),
                EarnTxStatus(TxInfo.TransactionStatus.Confirmed, EarnStatusDetail.AwaitingSettlement(FAKE_INSTANT)),
                EarnTxStatus(TxInfo.TransactionStatus.Confirmed, EarnStatusDetail.Received(FAKE_POSITION)),
            )
        } else {
            flowOf(
                EarnTxStatus(TxInfo.TransactionStatus.Unconfirmed),
                EarnTxStatus(TxInfo.TransactionStatus.Confirmed),
            )
        }
    }

    private fun providerName(): String = when (preset) {
        FakeEarnSessionPreset.Lending -> "Aave"
        FakeEarnSessionPreset.Vault -> "Gauntlet"
    }

    private fun EarnFlowAction.isSupported(): Boolean = this is Deposit || this is Withdraw || this is FakeClaim

    private fun EarnAmount?.resolve(entire: BigDecimal): BigDecimal? = when (this) {
        is EarnAmount.Exact -> value
        EarnAmount.Entire -> entire
        null -> null
    }

    private fun depositPlan(amount: BigDecimal, fee: EarnFeeSelection) = FakeDepositPlan(
        fee = fee.fee,
        expectedAmount = amount,
        rows = listOf(
            row(label = "Current APY", value = "5.5%"),
            row(label = "Performance fee", value = "15%"),
            row(label = "Projected earnings", value = "~\$100/year"),
        ),
        withdrawMode = WithdrawMode.Instant,
        quoteTtl = null,
        notice = null,
        isEstimate = true,
        actionId = Deposit.ID,
    )

    private fun withdrawPlan(amount: BigDecimal, fee: EarnFeeSelection) = FakeDepositPlan(
        fee = fee.fee,
        expectedAmount = amount,
        rows = emptyList(),
        withdrawMode = when (preset) {
            FakeEarnSessionPreset.Lending -> WithdrawMode.Instant
            FakeEarnSessionPreset.Vault -> WithdrawMode.Queued(FAKE_INSTANT)
        },
        quoteTtl = if (preset == FakeEarnSessionPreset.Vault) FAKE_QUOTE_TTL else null,
        notice = stringReference(fakeWithdrawNotice(preset)),
        isEstimate = false,
        actionId = Withdraw.ID,
    )

    private fun claimPlan(fee: EarnFeeSelection) = FakeDepositPlan(
        fee = fee.fee,
        expectedAmount = null,
        rows = emptyList(),
        withdrawMode = null,
        quoteTtl = null,
        notice = null,
        isEstimate = false,
        actionId = FakeClaim.id,
    )

    private fun resultTitle(plan: FakeDepositPlan): String {
        val amount = plan.expectedAmount?.toPlainString()
        return when (plan.actionId) {
            Deposit.ID -> "Deposited $amount ETH"
            Withdraw.ID -> if (preset == FakeEarnSessionPreset.Vault) {
                "Withdrawal requested"
            } else {
                "Withdrawn $amount ETH"
            }
            else -> "Claimed"
        }
    }

    private fun withdrawPeriod(): String = when (preset) {
        FakeEarnSessionPreset.Lending -> "Usually instant"
        FakeEarnSessionPreset.Vault -> "Up to 7 days"
    }

    private fun row(label: String, value: String) = EarnRow(
        label = stringReference(label),
        value = EarnValue.Text(stringReference(value)),
    )
}