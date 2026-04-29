package com.tangem.domain.account.status.usecase

import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.pay.flow.PaymentAccountStatusProducer
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

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
    operator fun invoke(): Flow<Boolean> {
        val cryptoMode = multiAccountListSupplier.invoke()
            .map { lists -> lists.any { it.hasMultipleCryptoPortfolios() } }

        val paymentMode = multiAccountListSupplier.invoke().flatMapLatest { lists ->
            val walletIdsWithPayment = lists.mapNotNull { list ->
                if (list.accounts.any { it is Account.Payment }) list.userWalletId else null
            }

            if (walletIdsWithPayment.isEmpty()) {
                flowOf(false)
            } else {
                val flows = walletIdsWithPayment.map { walletId ->
                    paymentAccountStatusSupplier.invoke(walletId)
                        .map { it.value.isActivePayment() }
                        .onStart { emit(false) }
                }
                combine(flows) { results -> results.any { it } }
            }
        }

        return combine(cryptoMode, paymentMode) { crypto, payment -> crypto || payment }
            .distinctUntilChanged()
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
    }
}