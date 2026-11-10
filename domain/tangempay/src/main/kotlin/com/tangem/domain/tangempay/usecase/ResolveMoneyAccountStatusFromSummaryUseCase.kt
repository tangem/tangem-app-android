package com.tangem.domain.tangempay.usecase

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.account.MoneyAccountStatusValue.Onboarding.Activating.Step as ActivatingStep
import com.tangem.domain.tangempay.model.CustomerSummary
import com.tangem.domain.tangempay.model.MoneyAccountActivationState
import com.tangem.domain.tangempay.model.TangemPayCustomerState
import com.tangem.domain.tangempay.utils.TangemPayCustomerStateConverter

class ResolveMoneyAccountStatusFromSummaryUseCase {

    operator fun invoke(summary: CustomerSummary): MoneyAccountStatusValue? {
        return when (val state = TangemPayCustomerStateConverter.convert(summary)) {
            TangemPayCustomerState.KycRequired,
            TangemPayCustomerState.KycInProgress,
            TangemPayCustomerState.KycDeclined,
            TangemPayCustomerState.KycApproved,
            TangemPayCustomerState.Blocked,
            -> {
                MoneyAccountStatusValue.Empty
            }
            is TangemPayCustomerState.Former -> {
                account(state.moneyAccount, summary, isFormer = true)
            }
            is TangemPayCustomerState.Accounts,
            -> {
                account(state.moneyAccount, summary, isFormer = false)
            }
        }
    }

    private fun account(
        state: MoneyAccountActivationState,
        summary: CustomerSummary,
        isFormer: Boolean,
    ): MoneyAccountStatusValue? {
        return when (state) {
            MoneyAccountActivationState.NotStarted,
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
                step = when (state.step) {
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