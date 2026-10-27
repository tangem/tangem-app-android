package com.tangem.domain.moneyaccount.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.*
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

    private companion object {
        val POLLING_INTERVAL = 30.seconds

        val PaymentAccountActivationState.isTerminal: Boolean
            get() = when (this) {
                is PaymentAccountActivationState.Issuing -> false
                is PaymentAccountActivationState.AwaitingKyc -> !kycStatus.isInProgress

                PaymentAccountActivationState.NotStarted,
                PaymentAccountActivationState.Unavailable,
                is PaymentAccountActivationState.Failed,
                is PaymentAccountActivationState.Completed,
                -> true
            }

        val MoneyAccountActivationState.isTerminal: Boolean
            get() = when (this) {
                MoneyAccountActivationState.Issuing -> false
                is MoneyAccountActivationState.AwaitingKyc -> !kycStatus.isInProgress

                MoneyAccountActivationState.NotStarted,
                MoneyAccountActivationState.Unavailable,
                is MoneyAccountActivationState.Failed,
                MoneyAccountActivationState.Completed,
                MoneyAccountActivationState.Deactivated,
                -> true
            }

        val TangemPayCustomerState.isTerminal: Boolean
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
    }
}