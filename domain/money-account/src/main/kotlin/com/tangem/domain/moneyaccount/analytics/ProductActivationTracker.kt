package com.tangem.domain.moneyaccount.analytics

import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.model.MoneyAccountActivationState
import com.tangem.domain.pay.model.PaymentAccountActivationState
import com.tangem.domain.pay.model.TangemPayCustomerState
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.ActivationFailureReason
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.ActivationResult
import kotlin.time.TimeMark

internal class ProductActivationTracker(
    orderedProducts: Set<TangemPayProduct>,
    private val plan: TangemPayTariffPlan?,
    private val orderedAt: TimeMark,
) {
    private val unfinished = orderedProducts.toMutableSet()

    fun onState(state: TangemPayCustomerState): List<TangemPayAnalyticsEvents.ProductActivationFinished> {
        val finished = unfinished.mapNotNull { product ->
            state.outcomeOf(product)?.let { outcome -> product to outcome }
        }
        unfinished -= finished.map { (product, _) -> product }
        return finished.map { (product, outcome) -> event(product, outcome) }
    }

    fun onPollingStopped(): List<TangemPayAnalyticsEvents.ProductActivationFinished> {
        val stopped = unfinished.map { product -> event(product, Outcome(result = ActivationResult.POLLING_STOPPED)) }
        unfinished.clear()
        return stopped
    }

    private fun event(product: TangemPayProduct, outcome: Outcome) = TangemPayAnalyticsEvents.ProductActivationFinished(
        product = product.toAnalyticsProduct(),
        result = outcome.result,
        failureReason = outcome.failureReason,
        sinceOrder = orderedAt.elapsedNow(),
        plan = plan,
    )

    private fun TangemPayCustomerState.outcomeOf(product: TangemPayProduct): Outcome? = when (this) {
        is TangemPayCustomerState.Accounts -> product.outcomeIn(paymentAccount, moneyAccount)
        is TangemPayCustomerState.Former -> product.outcomeIn(paymentAccount, moneyAccount)
        TangemPayCustomerState.Blocked,
        TangemPayCustomerState.KycDeclined,
        -> failedForOtherReason()
        TangemPayCustomerState.KycRequired,
        TangemPayCustomerState.KycInProgress,
        TangemPayCustomerState.KycApproved,
        -> null
    }

    private fun TangemPayProduct.outcomeIn(
        paymentAccount: PaymentAccountActivationState,
        moneyAccount: MoneyAccountActivationState,
    ): Outcome? = when (this) {
        TangemPayProduct.PAYMENT_ACCOUNT -> paymentAccount.toOutcome()
        TangemPayProduct.BANKING_ACCOUNT -> moneyAccount.toOutcome()
    }

    private fun PaymentAccountActivationState.toOutcome(): Outcome? = when (this) {
        is PaymentAccountActivationState.Completed -> Outcome(result = ActivationResult.ACTIVE)
        is PaymentAccountActivationState.Failed -> when (reason) {
            CustomerSummary.PaymentAccount.Activation.FailureReason.DEPOSIT_TIMEOUT -> Outcome(
                result = ActivationResult.TIMED_OUT,
                failureReason = ActivationFailureReason.PLUS_DEPOSIT_TIMEOUT,
            )
            CustomerSummary.PaymentAccount.Activation.FailureReason.RAIN_REJECTED -> issuerRejected()
            CustomerSummary.PaymentAccount.Activation.FailureReason.TECHNICAL,
            CustomerSummary.PaymentAccount.Activation.FailureReason.UNKNOWN,
            -> failedForOtherReason()
        }
        PaymentAccountActivationState.Unavailable -> failedForOtherReason()
        is PaymentAccountActivationState.AwaitingKyc -> kycStatus.toOutcome()
        PaymentAccountActivationState.NotStarted,
        is PaymentAccountActivationState.Issuing,
        -> null
    }

    private fun MoneyAccountActivationState.toOutcome(): Outcome? = when (this) {
        MoneyAccountActivationState.Completed -> Outcome(result = ActivationResult.ACTIVE)
        is MoneyAccountActivationState.Failed -> when (reason) {
            CustomerSummary.MoneyAccount.Activation.FailureReason.RAIN_REJECTED -> issuerRejected()
            CustomerSummary.MoneyAccount.Activation.FailureReason.TECHNICAL,
            CustomerSummary.MoneyAccount.Activation.FailureReason.UNKNOWN,
            -> failedForOtherReason()
        }
        MoneyAccountActivationState.Unavailable -> failedForOtherReason()
        is MoneyAccountActivationState.AwaitingKyc -> kycStatus.toOutcome()
        MoneyAccountActivationState.NotStarted,
        MoneyAccountActivationState.Issuing,
        MoneyAccountActivationState.Deactivated,
        -> null
    }

    private fun CustomerSummary.KycStatus.toOutcome(): Outcome? =
        if (this == CustomerSummary.KycStatus.DECLINED) failedForOtherReason() else null

    private fun issuerRejected() = Outcome(
        result = ActivationResult.FAILED,
        failureReason = ActivationFailureReason.ISSUER_REJECTED,
    )

    private fun failedForOtherReason() = Outcome(
        result = ActivationResult.FAILED,
        failureReason = ActivationFailureReason.OTHER,
    )

    private fun TangemPayProduct.toAnalyticsProduct(): TangemPayAnalyticsEvents.Product = when (this) {
        TangemPayProduct.PAYMENT_ACCOUNT -> TangemPayAnalyticsEvents.Product.CARD
        TangemPayProduct.BANKING_ACCOUNT -> TangemPayAnalyticsEvents.Product.ACCOUNT
    }

    private data class Outcome(
        val result: ActivationResult,
        val failureReason: ActivationFailureReason? = null,
    )
}