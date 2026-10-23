package com.tangem.domain.moneyaccount.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.CachePolicy
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.model.MoneyAccountActivationState
import com.tangem.domain.pay.model.PaymentAccountActivationState
import com.tangem.domain.pay.model.TangemPayCustomerState
import com.tangem.domain.pay.usecase.GetCustomerSummaryUseCase
import com.tangem.domain.pay.utils.TangemPayCustomerStateConverter
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

// TODO v_rodionov: [REDACTED_TASK_KEY] Rename it
class RestoreMoneyAccountOrderUseCase(
    private val getCustomerSummaryUseCase: GetCustomerSummaryUseCase,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher,
) {
    suspend operator fun invoke(userWalletId: UserWalletId) {
        while (true) {
            fetchAccountStatuses(userWalletId)
            val summary = getCustomerSummaryUseCase(userWalletId, CachePolicy.ONLY_CACHE).getOrNull()
            if (summary != null && TangemPayCustomerStateConverter.convert(summary).isTerminal) return
            delay(POLLING_INTERVAL)
        }
    }

    private suspend fun fetchAccountStatuses(userWalletId: UserWalletId) {
        coroutineScope {
            launch { paymentAccountStatusFetcher.invoke(PaymentAccountStatusFetcher.Params(userWalletId)) }
            launch { moneyAccountStatusFetcher.invoke(userWalletId) }
        }
    }

    private val TangemPayCustomerState.isTerminal: Boolean
        get() = when (this) {
            TangemPayCustomerState.KycInProgress -> false
            is TangemPayCustomerState.Accounts -> paymentAccount.isTerminal && moneyAccount.isTerminal

            TangemPayCustomerState.Hidden,
            TangemPayCustomerState.Blocked,
            TangemPayCustomerState.KycRequired,
            TangemPayCustomerState.KycDeclined,
            TangemPayCustomerState.KycApproved,
            is TangemPayCustomerState.Former,
            -> true
        }

    private val PaymentAccountActivationState.isTerminal: Boolean
        get() = when (this) {
            is PaymentAccountActivationState.Issuing -> false
            is PaymentAccountActivationState.AwaitingKyc -> !kycStatus.isUnderReview

            PaymentAccountActivationState.NotStarted,
            PaymentAccountActivationState.Unavailable,
            is PaymentAccountActivationState.Failed,
            is PaymentAccountActivationState.Completed,
            -> true
        }

    private val MoneyAccountActivationState.isTerminal: Boolean
        get() = when (this) {
            MoneyAccountActivationState.Issuing -> false
            is MoneyAccountActivationState.AwaitingKyc -> !kycStatus.isUnderReview

            MoneyAccountActivationState.NotStarted,
            MoneyAccountActivationState.Unavailable,
            is MoneyAccountActivationState.Failed,
            MoneyAccountActivationState.Completed,
            MoneyAccountActivationState.Deactivated,
            -> true
        }

    private val CustomerSummary.KycStatus.isUnderReview: Boolean
        get() = when (this) {
            CustomerSummary.KycStatus.IN_PROGRESS,
            CustomerSummary.KycStatus.UNKNOWN,
            -> true
            CustomerSummary.KycStatus.REQUIRED,
            CustomerSummary.KycStatus.DECLINED,
            CustomerSummary.KycStatus.APPROVED,
            -> false
        }

    private companion object {
        val POLLING_INTERVAL = 30.seconds
    }
}