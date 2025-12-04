package com.tangem.data.staking.utils

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.data.staking.store.StakingBalancesStore
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.utils.StakingCleaner
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Default implementation of [StakingCleaner].
 *
 * @property stakingBalancesStore Store to manage staking balances.
 * @property dispatchers Coroutine dispatchers provider.
 *
[REDACTED_AUTHOR]
 */
internal class DefaultStakingCleaner(
    private val stakingIdFactory: StakingIdFactory,
    private val stakingBalancesStore: StakingBalancesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : StakingCleaner {

    override suspend fun invoke(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        if (currencies.isEmpty()) {
            Timber.d("No currencies to clear for wallet: $userWalletId")
            return
        }

        val stakingIds = currencies.mapNotNullTo(hashSetOf()) {
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = it).getOrNull()
        }

        if (stakingIds.isEmpty()) {
            Timber.d("All currencies have no stakingIds to clear for wallet: $userWalletId")
            return
        }

        invoke(userWalletId = userWalletId, stakingIds = stakingIds)
    }

    override suspend fun invoke(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        if (stakingIds.isEmpty()) {
            Timber.d("No stakingIds to clear for wallet: $userWalletId")
            return
        }

        withContext(dispatchers.default) {
            awaitAll(
                async { clearStatusesStore(userWalletId = userWalletId, stakingIds = stakingIds) },
            )
        }
    }

    private suspend fun clearStatusesStore(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        runSuspendCatching {
            stakingBalancesStore.clear(userWalletId, stakingIds)
        }
            .onFailure { Timber.e(it, "Failed to clear yield balance statuses for wallet: $userWalletId") }
    }
}