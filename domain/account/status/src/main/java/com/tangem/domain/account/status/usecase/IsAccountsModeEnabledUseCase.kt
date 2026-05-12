package com.tangem.domain.account.status.usecase

import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.pay.flow.PaymentAccountStatusProducer
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import com.tangem.utils.logging.TangemLogger

/**
 * Use case to determine if the accounts mode is enabled.
 *
 * Accounts mode is considered enabled if any user wallet either:
 *  - contains more than one [Account.CryptoPortfolio], or
 *  - has a [com.tangem.domain.models.account.AccountStatus.Payment] in an active state.
 *
 * Splits the structural check (via [MultiAccountListSupplier]) and the payment-status check
 * (via [PaymentAccountStatusSupplier] per wallet) so that the structural answer doesn't wait for
 * slow per-wallet status pipelines on cold start.
 */
class IsAccountsModeEnabledUseCase(
    private val multiAccountListSupplier: MultiAccountListSupplier,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Boolean> = channelFlow {
        TangemLogger.i("$TAG: invoke() started")

        val sharedLists = multiAccountListSupplier.invoke()
            .onEach { lists ->
                TangemLogger.i("$TAG: multiAccountListSupplier emitted ${lists.size} lists")
            }
            .shareIn(this, SharingStarted.Lazily, replay = 1)

        val cryptoMode = sharedLists
            .map { lists ->
                val isCryptoMode = lists.any { it.hasMultipleCryptoPortfolios() }
                TangemLogger.i("$TAG: cryptoMode=$isCryptoMode")
                isCryptoMode
            }

        val paymentMode = sharedLists.flatMapLatest { lists ->
            val walletIdsWithPayment = lists.mapNotNull { list ->
                if (list.accounts.any { it is Account.Payment }) list.userWalletId else null
            }
            TangemLogger.i("$TAG: walletIdsWithPayment=${walletIdsWithPayment.size}")

            if (walletIdsWithPayment.isEmpty()) {
                flowOf(false)
            } else {
                val flows = walletIdsWithPayment.map { walletId ->
                    paymentAccountStatusSupplier.invoke(walletId)
                        .onEach { status ->
                            TangemLogger.i("$TAG: paymentStatus for $walletId = ${status.value}")
                        }
                        .map { it.value.isActivePayment() }
                        .onStart {
                            TangemLogger.i("$TAG: paymentAccountStatusSupplier onStart for $walletId")
                            emit(false)
                        }
                }
                combine(flows) { results ->
                    val isPaymentMode = results.any { it }
                    TangemLogger.i("$TAG: paymentMode combine result=$isPaymentMode (${results.toList()})")
                    isPaymentMode
                }
            }
        }

        combine(cryptoMode, paymentMode) { crypto, payment ->
            val isEnabled = crypto || payment
            TangemLogger.i("$TAG: final combine crypto=$crypto, payment=$payment, result=$isEnabled")
            isEnabled
        }
            .distinctUntilChanged()
            .collect { send(it) }
    }

    suspend fun invokeSync(): Boolean {
        val accountLists = multiAccountListSupplier.getSyncOrNull(Unit).orEmpty()
        if (accountLists.any { it.hasMultipleCryptoPortfolios() }) return true

        val walletIdsWithPayment = accountLists.mapNotNull { list ->
            if (list.accounts.any { it is Account.Payment }) list.userWalletId else null
        }
        return walletIdsWithPayment.any { walletId ->
            paymentAccountStatusSupplier
                .getSyncOrNull(
                    params = PaymentAccountStatusProducer.Params(walletId),
                    timeMillis = PAYMENT_STATUS_SYNC_TIMEOUT_MS,
                )
                ?.value
                ?.isActivePayment() == true
        }
    }

    private fun AccountList.hasMultipleCryptoPortfolios(): Boolean =
        accounts.filterIsInstance<Account.CryptoPortfolio>().size > 1

    private fun PaymentAccountStatusValue.isActivePayment(): Boolean {
        return when (this) {
            is PaymentAccountStatusValue.Empty,
            is PaymentAccountStatusValue.NotCreated,
            -> false
            is PaymentAccountStatusValue.Error,
            is PaymentAccountStatusValue.IssuingCard,
            is PaymentAccountStatusValue.Loaded,
            is PaymentAccountStatusValue.Loading,
            is PaymentAccountStatusValue.Locked,
            is PaymentAccountStatusValue.UnderReview,
            is PaymentAccountStatusValue.Deactivated,
            -> true
        }
    }

    private companion object {
        const val PAYMENT_STATUS_SYNC_TIMEOUT_MS = 1_000L
        const val TAG = "IsAccountsModeEnabledUseCase"
    }
}