package com.tangem.domain.tangempay.usecase

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.account.MoneyAccountStatusValue.Onboarding.Activating.Step as ActivatingStep
import com.tangem.domain.tangempay.model.CustomerSummary
import com.tangem.domain.tangempay.model.MoneyAccountActivationState
import com.tangem.domain.tangempay.model.PaymentAccountActivationState
import com.tangem.domain.tangempay.model.TangemPayCustomerState
import com.tangem.domain.tangempay.utils.TangemPayCustomerStateConverter

class ResolveMoneyAccountStatusFromSummaryUseCase {

    operator fun invoke(summary: CustomerSummary): MoneyAccountStatusValue? {
        val state = TangemPayCustomerStateConverter.convert(summary)
        return when (state) {
            TangemPayCustomerState.KycRequired,
            TangemPayCustomerState.KycInProgress,
            TangemPayCustomerState.KycDeclined,
            TangemPayCustomerState.KycApproved,
            TangemPayCustomerState.Blocked,
            -> {
                MoneyAccountStatusValue.Empty
            }
            is TangemPayCustomerState.Former -> {
                account(
                    moneyAccount = state.moneyAccount,
                    paymentAccount = state.paymentAccount,
                    summary = summary,
                    isFormer = true,
                )
            }
            is TangemPayCustomerState.Accounts,
            -> {
                account(
                    moneyAccount = state.moneyAccount,
                    paymentAccount = state.paymentAccount,
                    summary = summary,
                    isFormer = false,
                )
            }
        }
    }

    private fun account(
        paymentAccount: PaymentAccountActivationState,
        moneyAccount: MoneyAccountActivationState,
        summary: CustomerSummary,
        isFormer: Boolean,
    ): MoneyAccountStatusValue? {
        return when (moneyAccount) {
            MoneyAccountActivationState.NotStarted -> {
                val hasPayAccount = paymentAccount is PaymentAccountActivationState.Completed
                if (isFormer || !hasPayAccount) {
                    MoneyAccountStatusValue.Empty
                } else {
                    MoneyAccountStatusValue.Onboarding.NotStarted(
                        source = StatusSource.ACTUAL,
                        customerId = summary.customerId,
                    )
                }
            }

            MoneyAccountActivationState.Unavailable,
            is MoneyAccountActivationState.AwaitingKyc,
            -> {
                MoneyAccountStatusValue.Empty
            }

            is MoneyAccountActivationState.Failed -> if (isFormer) {
                MoneyAccountStatusValue.Empty
            } else {
                MoneyAccountStatusValue.Onboarding.Failed(
                    source = StatusSource.ACTUAL,
                    customerId = summary.customerId,
                )
            }

            is MoneyAccountActivationState.Issuing -> MoneyAccountStatusValue.Onboarding.Activating(
                source = StatusSource.ACTUAL,
                customerId = summary.customerId,
                step = when (moneyAccount.step) {
                    MoneyAccountActivationState.Issuing.Step.ISSUING -> ActivatingStep.ISSUING
                    MoneyAccountActivationState.Issuing.Step.AWAITING_DEPOSIT -> ActivatingStep.AWAITING_DEPOSIT
                },
            )

            MoneyAccountActivationState.Deactivated,
            MoneyAccountActivationState.Completed,
            -> null
        }
    }
}