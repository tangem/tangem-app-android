package com.tangem.domain.tangempay.utils

import com.tangem.domain.tangempay.model.CustomerSummary
import com.tangem.domain.tangempay.model.CustomerSummary.*
import com.tangem.domain.tangempay.model.MoneyAccountActivationState
import com.tangem.domain.tangempay.model.MoneyAccountActivationState.Issuing.Step as MoneyAccountIssuingStep
import com.tangem.domain.tangempay.model.PaymentAccountActivationState
import com.tangem.domain.tangempay.model.PaymentAccountActivationState.Issuing
import com.tangem.domain.tangempay.model.TangemPayCustomerState

object TangemPayCustomerStateConverter {

    fun convert(value: CustomerSummary, isKycAdditionalInfoEnabled: Boolean): TangemPayCustomerState {
        val isKycAdditionalInfoRequested = isKycAdditionalInfoEnabled && value.isKycAdditionalInfoRequested
        val isKycGateOpen = value.kycStatus == KycStatus.APPROVED || isKycAdditionalInfoRequested

        when (value.customerStatus) {
            CustomerStatus.BLOCKED -> return TangemPayCustomerState.Blocked
            CustomerStatus.FORMER -> return TangemPayCustomerState.Former(
                paymentAccount = convertPaymentAccount(
                    account = value.paymentAccount,
                    kycStatus = value.kycStatus,
                    isKycGateOpen = isKycGateOpen,
                ),
                moneyAccount = convertMoneyAccount(
                    activation = value.moneyAccount.activation,
                    kycStatus = value.kycStatus,
                    isKycGateOpen = isKycGateOpen,
                ),
            )
            CustomerStatus.NEW,
            CustomerStatus.ACTIVE,
            CustomerStatus.UNKNOWN,
            -> Unit
        }

        val paymentActivation = value.paymentAccount.activation
        val moneyActivation = value.moneyAccount.activation

        val isPaymentAccountPending = isPending(status = paymentActivation.status, isKycGateOpen = isKycGateOpen)
        val isMoneyAccountPending = isPending(status = moneyActivation.status, isKycGateOpen = isKycGateOpen)

        if (isPaymentAccountPending && isMoneyAccountPending && !isKycAdditionalInfoRequested) {
            return convertKyc(value.kycStatus)
        }

        return TangemPayCustomerState.Accounts(
            paymentAccount = convertPaymentAccount(
                account = value.paymentAccount,
                kycStatus = value.kycStatus,
                isKycGateOpen = isKycGateOpen,
            ),
            moneyAccount = convertMoneyAccount(
                activation = moneyActivation,
                kycStatus = value.kycStatus,
                isKycGateOpen = isKycGateOpen,
            ),
        )
    }

    private fun isPending(status: PaymentAccount.Activation.Status, isKycGateOpen: Boolean): Boolean = when (status) {
        PaymentAccount.Activation.Status.NOT_STARTED,
        PaymentAccount.Activation.Status.UNAVAILABLE,
        -> true
        PaymentAccount.Activation.Status.PROCESSING,
        PaymentAccount.Activation.Status.UNKNOWN,
        -> !isKycGateOpen
        PaymentAccount.Activation.Status.FAILED,
        PaymentAccount.Activation.Status.COMPLETED,
        -> false
    }

    private fun isPending(status: MoneyAccount.Activation.Status, isKycGateOpen: Boolean): Boolean = when (status) {
        MoneyAccount.Activation.Status.NOT_STARTED,
        MoneyAccount.Activation.Status.UNAVAILABLE,
        -> true
        MoneyAccount.Activation.Status.PROCESSING,
        MoneyAccount.Activation.Status.UNKNOWN,
        -> !isKycGateOpen
        MoneyAccount.Activation.Status.FAILED,
        MoneyAccount.Activation.Status.COMPLETED,
        MoneyAccount.Activation.Status.DEACTIVATED,
        -> false
    }

    private fun convertKyc(kycStatus: KycStatus): TangemPayCustomerState = when (kycStatus) {
        KycStatus.REQUIRED -> TangemPayCustomerState.KycRequired
        KycStatus.IN_PROGRESS,
        KycStatus.UNKNOWN,
        -> TangemPayCustomerState.KycInProgress
        KycStatus.DECLINED -> TangemPayCustomerState.KycDeclined
        KycStatus.APPROVED -> TangemPayCustomerState.KycApproved
    }

    private fun convertPaymentAccount(
        account: PaymentAccount,
        kycStatus: KycStatus,
        isKycGateOpen: Boolean,
    ): PaymentAccountActivationState {
        val activation = account.activation
        return when (activation.status) {
            PaymentAccount.Activation.Status.COMPLETED -> PaymentAccountActivationState.Completed(cards = account.cards)
            PaymentAccount.Activation.Status.FAILED -> PaymentAccountActivationState.Failed(
                reason = activation.failureReason ?: PaymentAccount.Activation.FailureReason.TECHNICAL,
            )
            PaymentAccount.Activation.Status.UNAVAILABLE -> PaymentAccountActivationState.Unavailable
            PaymentAccount.Activation.Status.NOT_STARTED -> if (isKycGateOpen) {
                PaymentAccountActivationState.NotStarted
            } else {
                PaymentAccountActivationState.AwaitingKyc(kycStatus)
            }
            PaymentAccount.Activation.Status.PROCESSING,
            PaymentAccount.Activation.Status.UNKNOWN,
            -> when {
                !isKycGateOpen -> PaymentAccountActivationState.AwaitingKyc(kycStatus)
                activation.step == PaymentAccount.Activation.Step.AWAITING_DEPOSIT -> {
                    Issuing(step = Issuing.Step.AWAITING_DEPOSIT)
                }
                else -> Issuing(step = Issuing.Step.ISSUING)
            }
        }
    }

    private fun convertMoneyAccount(
        activation: MoneyAccount.Activation,
        kycStatus: KycStatus,
        isKycGateOpen: Boolean,
    ): MoneyAccountActivationState {
        return when (activation.status) {
            MoneyAccount.Activation.Status.COMPLETED -> MoneyAccountActivationState.Completed
            MoneyAccount.Activation.Status.DEACTIVATED -> MoneyAccountActivationState.Deactivated
            MoneyAccount.Activation.Status.FAILED -> MoneyAccountActivationState.Failed(
                reason = activation.failureReason ?: MoneyAccount.Activation.FailureReason.TECHNICAL,
            )
            MoneyAccount.Activation.Status.UNAVAILABLE -> MoneyAccountActivationState.Unavailable
            MoneyAccount.Activation.Status.NOT_STARTED -> if (isKycGateOpen) {
                MoneyAccountActivationState.NotStarted
            } else {
                MoneyAccountActivationState.AwaitingKyc(kycStatus)
            }
            MoneyAccount.Activation.Status.PROCESSING,
            MoneyAccount.Activation.Status.UNKNOWN,
            -> when {
                !isKycGateOpen -> MoneyAccountActivationState.AwaitingKyc(kycStatus)
                activation.step == MoneyAccount.Activation.Step.AWAITING_DEPOSIT -> {
                    MoneyAccountActivationState.Issuing(step = MoneyAccountIssuingStep.AWAITING_DEPOSIT)
                }
                else -> MoneyAccountActivationState.Issuing(step = MoneyAccountIssuingStep.ISSUING)
            }
        }
    }
}