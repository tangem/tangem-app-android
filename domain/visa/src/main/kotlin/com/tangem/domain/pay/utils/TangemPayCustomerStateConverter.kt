package com.tangem.domain.pay.utils

import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.model.CustomerSummary.*
import com.tangem.domain.pay.model.MoneyAccountActivationState
import com.tangem.domain.pay.model.PaymentAccountActivationState
import com.tangem.domain.pay.model.PaymentAccountActivationState.Issuing
import com.tangem.domain.pay.model.TangemPayCustomerState
import com.tangem.utils.converter.Converter

object TangemPayCustomerStateConverter : Converter<CustomerSummary, TangemPayCustomerState> {

    override fun convert(value: CustomerSummary): TangemPayCustomerState {
        if (value.isHidden) return TangemPayCustomerState.Hidden

        when (value.customerStatus) {
            CustomerStatus.BLOCKED -> return TangemPayCustomerState.Blocked
            CustomerStatus.FORMER -> return TangemPayCustomerState.Former(
                paymentAccount = convertPaymentAccount(
                    account = value.paymentAccount,
                    kycStatus = value.kycStatus,
                ),
                moneyAccount = convertMoneyAccount(
                    activation = value.moneyAccount.activation,
                    kycStatus = value.kycStatus,
                ),
            )
            CustomerStatus.NEW,
            CustomerStatus.ACTIVE,
            CustomerStatus.UNKNOWN,
            -> Unit
        }

        val isKycApproved = value.kycStatus == KycStatus.APPROVED
        val paymentActivation = value.paymentAccount.activation
        val moneyActivation = value.moneyAccount.activation

        val isPaymentAccountPending = when (paymentActivation.status) {
            PaymentAccount.Activation.Status.NOT_STARTED,
            PaymentAccount.Activation.Status.UNAVAILABLE,
            -> true
            PaymentAccount.Activation.Status.PROCESSING,
            PaymentAccount.Activation.Status.UNKNOWN,
            -> !isKycApproved
            PaymentAccount.Activation.Status.FAILED,
            PaymentAccount.Activation.Status.COMPLETED,
            -> false
        }
        val isMoneyAccountPending = when (moneyActivation.status) {
            MoneyAccount.Activation.Status.NOT_STARTED,
            MoneyAccount.Activation.Status.UNAVAILABLE,
            -> true
            MoneyAccount.Activation.Status.PROCESSING,
            MoneyAccount.Activation.Status.UNKNOWN,
            -> !isKycApproved
            MoneyAccount.Activation.Status.FAILED,
            MoneyAccount.Activation.Status.COMPLETED,
            MoneyAccount.Activation.Status.DEACTIVATED,
            -> false
        }

        if (isPaymentAccountPending && isMoneyAccountPending) return convertKyc(value.kycStatus)

        return TangemPayCustomerState.Accounts(
            paymentAccount = convertPaymentAccount(account = value.paymentAccount, kycStatus = value.kycStatus),
            moneyAccount = convertMoneyAccount(activation = moneyActivation, kycStatus = value.kycStatus),
        )
    }

    private fun convertKyc(kycStatus: KycStatus): TangemPayCustomerState = when (kycStatus) {
        KycStatus.REQUIRED -> TangemPayCustomerState.KycRequired
        KycStatus.IN_PROGRESS,
        KycStatus.UNKNOWN,
        -> TangemPayCustomerState.KycInProgress
        KycStatus.DECLINED -> TangemPayCustomerState.KycDeclined
        KycStatus.APPROVED -> TangemPayCustomerState.KycApproved
    }

    private fun convertPaymentAccount(account: PaymentAccount, kycStatus: KycStatus): PaymentAccountActivationState {
        val isKycApproved = kycStatus == KycStatus.APPROVED
        val activation = account.activation
        return when (activation.status) {
            PaymentAccount.Activation.Status.COMPLETED -> PaymentAccountActivationState.Completed(cards = account.cards)
            PaymentAccount.Activation.Status.FAILED -> PaymentAccountActivationState.Failed(
                reason = activation.failureReason ?: PaymentAccount.Activation.FailureReason.TECHNICAL,
            )
            PaymentAccount.Activation.Status.UNAVAILABLE -> PaymentAccountActivationState.Unavailable
            PaymentAccount.Activation.Status.NOT_STARTED -> if (isKycApproved) {
                PaymentAccountActivationState.NotStarted
            } else {
                PaymentAccountActivationState.AwaitingKyc(kycStatus)
            }
            PaymentAccount.Activation.Status.PROCESSING,
            PaymentAccount.Activation.Status.UNKNOWN,
            -> when {
                !isKycApproved -> PaymentAccountActivationState.AwaitingKyc(kycStatus)
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
    ): MoneyAccountActivationState {
        val isKycApproved = kycStatus == KycStatus.APPROVED
        return when (activation.status) {
            MoneyAccount.Activation.Status.COMPLETED -> MoneyAccountActivationState.Completed
            MoneyAccount.Activation.Status.DEACTIVATED -> MoneyAccountActivationState.Deactivated
            MoneyAccount.Activation.Status.FAILED -> MoneyAccountActivationState.Failed(
                reason = activation.failureReason ?: MoneyAccount.Activation.FailureReason.TECHNICAL,
            )
            MoneyAccount.Activation.Status.UNAVAILABLE -> MoneyAccountActivationState.Unavailable
            MoneyAccount.Activation.Status.NOT_STARTED -> if (isKycApproved) {
                MoneyAccountActivationState.NotStarted
            } else {
                MoneyAccountActivationState.AwaitingKyc(kycStatus)
            }
            MoneyAccount.Activation.Status.PROCESSING,
            MoneyAccount.Activation.Status.UNKNOWN,
            -> if (isKycApproved) {
                MoneyAccountActivationState.Issuing
            } else {
                MoneyAccountActivationState.AwaitingKyc(kycStatus)
            }
        }
    }
}