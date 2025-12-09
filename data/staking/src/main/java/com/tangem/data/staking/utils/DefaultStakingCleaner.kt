package com.tangem.data.staking.utils

import com.tangem.data.staking.store.StakingBalancesStore
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.utils.StakingCleaner
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

/**
 * Default implementation of [StakingCleaner].
 *
 * @property stakingBalancesStore Store to manage staking balances.
 * @property dispatchers Coroutine dispatchers provider.
 *
[REDACTED_AUTHOR]
 */
internal class DefaultStakingCleaner(
    private val stakingBalancesStore: StakingBalancesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : StakingCleaner {

    override suspend fun invoke(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        if (stakingIds.isEmpty()) return

        with(dispatchers.default) {
            stakingBalancesStore.clear(userWalletId, stakingIds)
        }
    }
}