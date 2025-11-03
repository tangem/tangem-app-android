package com.tangem.data.staking.utils

import com.tangem.data.staking.store.YieldsBalancesStore
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.utils.StakingCleaner
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

/**
 * Default implementation of [StakingCleaner].
 *
 * @property yieldsBalancesStore Store to manage yields balances.
 * @property dispatchers Coroutine dispatchers provider.
 *
[REDACTED_AUTHOR]
 */
internal class DefaultStakingCleaner(
    private val yieldsBalancesStore: YieldsBalancesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : StakingCleaner {

    override suspend fun invoke(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        if (stakingIds.isEmpty()) return

        with(dispatchers.default) {
            yieldsBalancesStore.clear(userWalletId, stakingIds)
        }
    }
}