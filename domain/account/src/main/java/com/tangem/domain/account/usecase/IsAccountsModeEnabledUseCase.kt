package com.tangem.domain.account.usecase

import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Use case to determine if the accounts mode is enabled.
 * Accounts mode is considered enabled if any [AccountList] produced for the user's wallets contains at least two
 * active accounts.
 *
 * @property multiAccountListSupplier supplier that provides a list of [AccountList]s for all user wallets
 *
[REDACTED_AUTHOR]
 */
class IsAccountsModeEnabledUseCase(
    private val multiAccountListSupplier: MultiAccountListSupplier,
) {

    operator fun invoke(): Flow<Boolean> {
        return multiAccountListSupplier.invoke()
            .map { accountsList -> accountsList.map(AccountList::activeAccounts).isModeEnabled() }
            .distinctUntilChanged()
    }

    suspend fun invokeSync(): Boolean {
        return multiAccountListSupplier.getSyncOrNull(Unit)
            ?.map(AccountList::activeAccounts)
            ?.isModeEnabled() == true
    }

    private fun List<Int>.isModeEnabled(): Boolean = any { it >= 2 }
}