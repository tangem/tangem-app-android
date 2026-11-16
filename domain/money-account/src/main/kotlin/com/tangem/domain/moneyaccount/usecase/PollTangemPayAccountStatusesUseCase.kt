package com.tangem.domain.moneyaccount.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.*
import com.tangem.domain.pay.usecase.GetCustomerSummaryUseCase
import com.tangem.domain.pay.utils.TangemPayCustomerStateConverter
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class PollTangemPayAccountStatusesUseCase(
    private val getCustomerSummaryUseCase: GetCustomerSummaryUseCase,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher,
    private val timeSource: TimeSource,
) {
    operator fun invoke(userWalletId: UserWalletId): Flow<TangemPayCustomerState> = flow {
        val startedAt = timeSource.markNow()
        withTimeoutOrNull(MAX_POLLING_DURATION) {
            while (true) {
                fetchAccountStatuses(userWalletId)

                val state = getCustomerSummaryUseCase(userWalletId, CachePolicy.ONLY_CACHE)
                    .getOrNull()
                    ?.let(TangemPayCustomerStateConverter::convert)

                if (state != null) {
                    emit(state)
                    if (state.isTerminal) {
                        return@withTimeoutOrNull
                    }
                }

                delay(pollingInterval(startedAt.elapsedNow()))
            }
        }
    }

    private fun pollingInterval(elapsed: Duration): Duration =
        if (elapsed < FAST_POLLING_DURATION) FAST_POLLING_INTERVAL else SLOW_POLLING_INTERVAL

    private suspend fun fetchAccountStatuses(userWalletId: UserWalletId) {
        coroutineScope {
            launch { paymentAccountStatusFetcher.invoke(PaymentAccountStatusFetcher.Params(userWalletId)) }
            launch { moneyAccountStatusFetcher.invoke(userWalletId) }
        }
    }

    private companion object {
        val FAST_POLLING_INTERVAL = 10.seconds
        val FAST_POLLING_DURATION = 2.minutes
        val SLOW_POLLING_INTERVAL = 60.seconds
        val MAX_POLLING_DURATION = 15.minutes

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
                is MoneyAccountActivationState.Issuing -> false
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

                TangemPayCustomerState.Blocked,
                TangemPayCustomerState.KycRequired,
                TangemPayCustomerState.KycDeclined,
                TangemPayCustomerState.KycApproved,
                is TangemPayCustomerState.Former,
                -> true
            }
    }
}