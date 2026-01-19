package com.tangem.data.staking.utils

import com.tangem.data.staking.store.P2PEthPoolBalancesStore
import com.tangem.data.staking.store.StakeKitBalancesStore
import com.tangem.domain.models.currency.CryptoCurrency
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
 * @property stakeKitBalancesStore Store to manage StakeKit staking balances.
 * @property p2pEthPoolBalancesStore Store to manage P2PEthPool staking balances.
 * @property dispatchers Coroutine dispatchers provider.
 *
[REDACTED_AUTHOR]
 */
internal class DefaultStakingCleaner(
    private val stakingIdFactory: StakingIdFactory,
    private val stakeKitBalancesStore: StakeKitBalancesStore,
    private val p2pEthPoolBalancesStore: P2PEthPoolBalancesStore,
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
                async { clearStakeKitBalancesStore(userWalletId = userWalletId, stakingIds = stakingIds) },
                async { clearP2PEthPoolBalancesStore(userWalletId = userWalletId, stakingIds = stakingIds) },
            )
        }
    }

    private suspend fun clearStakeKitBalancesStore(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        runSuspendCatching {
            stakeKitBalancesStore.clear(userWalletId, stakingIds)
        }
            .onFailure { Timber.e(it, "Failed to clear StakeKit balance statuses for wallet: $userWalletId") }
    }

    private suspend fun clearP2PEthPoolBalancesStore(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        runSuspendCatching {
            p2pEthPoolBalancesStore.clear(userWalletId, stakingIds)
        }
            .onFailure { Timber.e(it, "Failed to clear P2PEthPool balance statuses for wallet: $userWalletId") }
    }
}