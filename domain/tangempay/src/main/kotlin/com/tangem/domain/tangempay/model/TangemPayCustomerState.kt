package com.tangem.domain.tangempay.model

sealed interface TangemPayCustomerState {

    data object Blocked : TangemPayCustomerState

    data object KycRequired : TangemPayCustomerState
    data object KycInProgress : TangemPayCustomerState
    data object KycDeclined : TangemPayCustomerState
    data object KycApproved : TangemPayCustomerState

    data class Accounts(
        val paymentAccount: PaymentAccountActivationState,
        val moneyAccount: MoneyAccountActivationState,
    ) : TangemPayCustomerState

    data class Former(
        val paymentAccount: PaymentAccountActivationState,
        val moneyAccount: MoneyAccountActivationState,
    ) : TangemPayCustomerState
}

sealed interface PaymentAccountActivationState {

    data object NotStarted : PaymentAccountActivationState
    data object Unavailable : PaymentAccountActivationState

    data class AwaitingKyc(val kycStatus: CustomerSummary.KycStatus) : PaymentAccountActivationState

    data class Issuing(val step: Step) : PaymentAccountActivationState {
        enum class Step {
            ISSUING,
            AWAITING_DEPOSIT,
        }
    }

    data class Failed(
        val reason: CustomerSummary.PaymentAccount.Activation.FailureReason,
    ) : PaymentAccountActivationState

    data class Completed(val cards: CustomerSummary.Cards) : PaymentAccountActivationState
}

sealed interface MoneyAccountActivationState {

    data object NotStarted : MoneyAccountActivationState
    data object Unavailable : MoneyAccountActivationState

    data class AwaitingKyc(val kycStatus: CustomerSummary.KycStatus) : MoneyAccountActivationState

    data class Issuing(val step: Step) : MoneyAccountActivationState {
        enum class Step {
            ISSUING,
            AWAITING_DEPOSIT,
        }
    }

    data class Failed(
        val reason: CustomerSummary.MoneyAccount.Activation.FailureReason,
    ) : MoneyAccountActivationState

    data object Completed : MoneyAccountActivationState
    data object Deactivated : MoneyAccountActivationState
}