package com.tangem.domain.account.usecase

import arrow.core.Option
import arrow.core.getOrElse
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
 * Use case to determine if the accounts mode is enabled.
 * Accounts mode is considered enabled if there are at least two accounts in any of the user wallets that support
 * multiple currencies.
 *
 * @property crudRepository repository to perform CRUD operations on accounts.
 * @property userWalletsListRepository repository to get the list of user wallets.
 * @property accountsFeatureToggles feature toggles for accounts.
 *
[REDACTED_AUTHOR]
 */
class IsAccountsModeEnabledUseCase(
    private val crudRepository: AccountsCRUDRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val accountsFeatureToggles: AccountsFeatureToggles,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Boolean> {
        if (!accountsFeatureToggles.isFeatureEnabled) return flowOf(value = false)

        return userWalletsListRepository.loadAndGet()
            .flatMapLatest { userWallets ->
                val totalAccountsCountList = getTotalAccountsCountList(userWallets)

                combine(flows = totalAccountsCountList) { it.toList().isModeEnabled() }
            }
            .onEmpty { emit(false) }
            .distinctUntilChanged()
    }

    suspend fun invokeSync(): Boolean {
        if (!accountsFeatureToggles.isFeatureEnabled) return false

        return userWalletsListRepository.userWallets.value.orEmpty()
            .map { userWallet ->
                // If the wallet does not support multiple currencies, we consider its account count as 0
                if (!userWallet.isMultiCurrency) return@map 0

                crudRepository.getTotalActiveAccountsCountSync(userWalletId = userWallet.walletId).getOrZero()
            }
            .isModeEnabled()
    }

    private fun getTotalAccountsCountList(userWallets: List<UserWallet>): List<Flow<Int>> {
        return userWallets
            .map { userWallet ->
                // If the wallet does not support multiple currencies, we consider its account count as 0
                if (!userWallet.isMultiCurrency) return@map flowOf(0)

                crudRepository.getTotalActiveAccountsCount(userWalletId = userWallet.walletId)
                    .map { maybeCount -> maybeCount.getOrZero() }
            }
    }

    private fun Option<Int>.getOrZero(): Int = getOrElse { 0 }

    private fun List<Int>.isModeEnabled(): Boolean = any { it >= 2 }
}