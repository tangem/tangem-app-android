package com.tangem.data.staking.store

import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Store for P2P ETH Pool staking balances
 */
interface P2PBalancesStore {

    fun get(userWalletId: UserWalletId): Flow<Set<YieldBalance>>

    suspend fun getSyncOrNull(userWalletId: UserWalletId, stakingId: StakingID): YieldBalance?

    suspend fun getAllSyncOrNull(userWalletId: UserWalletId): Set<YieldBalance>?

    suspend fun refresh(userWalletId: UserWalletId, stakingId: StakingID)

    suspend fun refresh(userWalletId: UserWalletId, stakingIds: Set<StakingID>)

    suspend fun storeActual(userWalletId: UserWalletId, values: Set<P2PEthPoolAccountResponse>)

    suspend fun storeError(userWalletId: UserWalletId, stakingIds: Set<StakingID>)

    suspend fun clear(userWalletId: UserWalletId, stakingIds: Set<StakingID>)
}