package com.tangem.domain.tangempay

import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.model.CustomerSummary.MoneyAccount
import com.tangem.domain.pay.model.CustomerSummary.PaymentAccount

object TangemPayUserProperties {

    fun of(summary: CustomerSummary): Map<String, String> = buildMap {
        summary.kycStatus.toValue()?.let { put("pay_kyc_status", it) }
        summary.paymentAccount.activation.toValue()?.let { put("pay_card_status", it) }
        summary.moneyAccount.activation.toValue()?.let { put("pay_account_status", it) }
    }

    fun of(tariffPlan: TangemPayCustomerTariffPlan?): Map<String, String> = mapOf(
        "pay_plan" to when {
            tariffPlan == null -> "none"
            tariffPlan.plan.isBasicTier -> "basic"
            else -> "plus"
        },
    )

    private fun CustomerSummary.KycStatus.toValue(): String? = when (this) {
        CustomerSummary.KycStatus.REQUIRED -> "required"
        CustomerSummary.KycStatus.IN_PROGRESS -> "in_progress"
        CustomerSummary.KycStatus.APPROVED -> "approved"
        CustomerSummary.KycStatus.DECLINED -> "declined"
        CustomerSummary.KycStatus.UNKNOWN -> null
    }

    private fun PaymentAccount.Activation.toValue(): String? = when (status) {
        PaymentAccount.Activation.Status.NOT_STARTED -> "not_started"
        PaymentAccount.Activation.Status.UNAVAILABLE -> "not_available"
        PaymentAccount.Activation.Status.PROCESSING -> when (step) {
            PaymentAccount.Activation.Step.AWAITING_KYC -> "under_review"
            PaymentAccount.Activation.Step.AWAITING_DEPOSIT -> "waiting_for_plus_deposit"
            PaymentAccount.Activation.Step.ISSUING,
            PaymentAccount.Activation.Step.UNKNOWN,
            null,
            -> "issuing"
        }
        PaymentAccount.Activation.Status.FAILED -> when (failureReason) {
            PaymentAccount.Activation.FailureReason.DEPOSIT_TIMEOUT -> "plus_not_funded"
            PaymentAccount.Activation.FailureReason.RAIN_REJECTED -> "verification_declined"
            PaymentAccount.Activation.FailureReason.TECHNICAL,
            PaymentAccount.Activation.FailureReason.UNKNOWN,
            null,
            -> "failed"
        }
        PaymentAccount.Activation.Status.COMPLETED -> "active"
        PaymentAccount.Activation.Status.UNKNOWN -> null
    }

    private fun MoneyAccount.Activation.toValue(): String? = when (status) {
        MoneyAccount.Activation.Status.NOT_STARTED -> "not_started"
        MoneyAccount.Activation.Status.UNAVAILABLE -> "not_available"
        MoneyAccount.Activation.Status.PROCESSING -> {
            if (step == MoneyAccount.Activation.Step.AWAITING_KYC) "under_review" else "issuing"
        }
        MoneyAccount.Activation.Status.FAILED -> when (failureReason) {
            MoneyAccount.Activation.FailureReason.RAIN_REJECTED -> "verification_declined"
            MoneyAccount.Activation.FailureReason.TECHNICAL,
            MoneyAccount.Activation.FailureReason.UNKNOWN,
            null,
            -> "failed"
        }
        MoneyAccount.Activation.Status.COMPLETED -> "active"
        MoneyAccount.Activation.Status.DEACTIVATED -> "closed"
        MoneyAccount.Activation.Status.UNKNOWN -> null
    }
}