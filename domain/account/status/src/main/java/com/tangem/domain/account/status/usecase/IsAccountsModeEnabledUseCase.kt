package com.tangem.domain.account.status.usecase

import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Use case to determine if the accounts mode is enabled.
 * Accounts mode is considered enabled if any [com.tangem.domain.account.models.AccountStatusList] produced for the
 * user's wallets has more than one [AccountStatus.CryptoPortfolio], or has a [AccountStatus.Payment] with any

 * [PaymentAccountStatusValue.Empty].
 *
 * @property multiAccountStatusListSupplier supplier that provides a list of
 * [com.tangem.domain.account.models.AccountStatusList]s for all user wallets
 *
[REDACTED_AUTHOR]
 */
class IsAccountsModeEnabledUseCase(
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
) {

    operator fun invoke(): Flow<Boolean> {
        return multiAccountStatusListSupplier.invoke()
            .map { accountStatusLists -> accountStatusLists.any { it.accountStatuses.isModeEnabled() } }
            .distinctUntilChanged()
    }

    suspend fun invokeSync(): Boolean {
        return multiAccountStatusListSupplier.getSyncOrNull(Unit)
            ?.any { it.accountStatuses.isModeEnabled() } == true
    }

    private fun List<AccountStatus>.isModeEnabled(): Boolean {
        var cryptoPortfolioCount = 0
        for (status in this) {
            when {
                status is AccountStatus.CryptoPortfolio && ++cryptoPortfolioCount > 1 -> return true
                status is AccountStatus.Payment && status.value.isActivePayment() -> return true
            }
        }
        return false
    }

    private fun PaymentAccountStatusValue.isActivePayment(): Boolean {
        return when (this) {
            is PaymentAccountStatusValue.Empty,
            is PaymentAccountStatusValue.NotCreated,
            -> false
            is PaymentAccountStatusValue.Error,
            is PaymentAccountStatusValue.IssuingCard,
            is PaymentAccountStatusValue.Loaded,
            is PaymentAccountStatusValue.Loading,
            is PaymentAccountStatusValue.UnderReview,
            -> true
        }
    }
}