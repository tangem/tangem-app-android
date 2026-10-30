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
import kotlin.time.Duration

internal object ProductActivationFinishedEvents {

    fun of(
        finalState: TangemPayCustomerState?,
        orderedProducts: Set<TangemPayProduct>,
        sinceOrder: Duration,
        plan: TangemPayTariffPlan?,
    ): List<TangemPayAnalyticsEvents.ProductActivationFinished> = orderedProducts.mapNotNull { product ->
        val outcome = if (finalState == null) {
            Outcome(result = ActivationResult.POLLING_STOPPED)
        } else {
            finalState.outcomeOf(product) ?: return@mapNotNull null
        }
        TangemPayAnalyticsEvents.ProductActivationFinished(
            product = product.toAnalyticsProduct(),
            result = outcome.result,
            failureReason = outcome.failureReason,
            sinceOrder = sinceOrder,
            plan = plan,
        )
    }

    private fun TangemPayCustomerState.outcomeOf(product: TangemPayProduct): Outcome? {
        val (paymentAccount, moneyAccount) = when (this) {
            is TangemPayCustomerState.Accounts -> paymentAccount to moneyAccount
            is TangemPayCustomerState.Former -> paymentAccount to moneyAccount
            TangemPayCustomerState.Blocked,
            TangemPayCustomerState.KycRequired,
            TangemPayCustomerState.KycInProgress,
            TangemPayCustomerState.KycDeclined,
            TangemPayCustomerState.KycApproved,
            -> return null
        }
        return when (product) {
            TangemPayProduct.PAYMENT_ACCOUNT -> paymentAccount.toOutcome()
            TangemPayProduct.BANKING_ACCOUNT -> moneyAccount.toOutcome()
        }
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
        PaymentAccountActivationState.NotStarted,
        is PaymentAccountActivationState.AwaitingKyc,
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
        MoneyAccountActivationState.NotStarted,
        is MoneyAccountActivationState.AwaitingKyc,
        MoneyAccountActivationState.Issuing,
        MoneyAccountActivationState.Deactivated,
        -> null
    }

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