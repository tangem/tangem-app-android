package com.tangem.domain.pay.usecase

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.model.MoneyAccountActivationState
import com.tangem.domain.pay.model.TangemPayCustomerState
import com.tangem.domain.pay.utils.TangemPayCustomerStateConverter

class ResolveMoneyAccountStatusFromSummaryUseCase {

    operator fun invoke(summary: CustomerSummary): MoneyAccountStatusValue? {
        return when (val state = TangemPayCustomerStateConverter.convert(summary)) {
            TangemPayCustomerState.KycRequired,
            TangemPayCustomerState.KycInProgress,
            TangemPayCustomerState.KycDeclined,
            TangemPayCustomerState.KycApproved,
            TangemPayCustomerState.Hidden,
            TangemPayCustomerState.Blocked,
            -> {
                MoneyAccountStatusValue.Empty
            }
            is TangemPayCustomerState.Former -> {
                account(state.moneyAccount, summary)
            }
            is TangemPayCustomerState.Accounts,
            -> {
                account(state.moneyAccount, summary)
            }
        }
    }

    private fun account(state: MoneyAccountActivationState, summary: CustomerSummary): MoneyAccountStatusValue? {
        return when (state) {
            MoneyAccountActivationState.NotStarted,
            MoneyAccountActivationState.Unavailable,
            is MoneyAccountActivationState.AwaitingKyc,
            -> {
                MoneyAccountStatusValue.Empty
            }

            MoneyAccountActivationState.Issuing -> {
                MoneyAccountStatusValue.Onboarding.Activating(
                    source = StatusSource.ACTUAL,
                    customerId = summary.customerId,
                )
            }

            is MoneyAccountActivationState.Failed -> {
                MoneyAccountStatusValue.Error.Unavailable
            }

            MoneyAccountActivationState.Deactivated,
            MoneyAccountActivationState.Completed,
            -> null
        }
    }
}