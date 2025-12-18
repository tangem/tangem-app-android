package com.tangem.data.staking.store

import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Store for P2PEthPool staking balances
 */
interface P2PEthPoolBalancesStore {

    fun get(userWalletId: UserWalletId): Flow<Set<StakingBalance>>

    suspend fun getSyncOrNull(userWalletId: UserWalletId, stakingId: StakingID): StakingBalance?

    suspend fun getAllSyncOrNull(userWalletId: UserWalletId): Set<StakingBalance>?

    suspend fun refresh(userWalletId: UserWalletId, stakingId: StakingID)

    suspend fun refresh(userWalletId: UserWalletId, stakingIds: Set<StakingID>)

    suspend fun storeActual(userWalletId: UserWalletId, values: Set<P2PEthPoolAccountResponse>)

    suspend fun storeError(userWalletId: UserWalletId, stakingIds: Set<StakingID>)

    suspend fun clear(userWalletId: UserWalletId, stakingIds: Set<StakingID>)
}