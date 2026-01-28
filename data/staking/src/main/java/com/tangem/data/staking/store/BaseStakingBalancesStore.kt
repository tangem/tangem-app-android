package com.tangem.data.staking.store

import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Base interface for staking balances stores.
 *
 * Defines common read/query operations shared by all staking provider stores.
 */
interface BaseStakingBalancesStore {

    /** Get flow of staking balances for a wallet */
    fun get(userWalletId: UserWalletId): Flow<Set<StakingBalance>>

    /** Get a single staking balance synchronously */
    suspend fun getSyncOrNull(userWalletId: UserWalletId, stakingId: StakingID): StakingBalance?

    /** Get all staking balances for a wallet synchronously */
    suspend fun getAllSyncOrNull(userWalletId: UserWalletId): Set<StakingBalance>?

    /** Refresh a single staking balance from cache */
    suspend fun refresh(userWalletId: UserWalletId, stakingId: StakingID)

    /** Refresh multiple staking balances from cache */
    suspend fun refresh(userWalletId: UserWalletId, stakingIds: Set<StakingID>)

    /** Store error state for staking balances */
    suspend fun storeError(userWalletId: UserWalletId, stakingIds: Set<StakingID>)

    /** Clear staking balances */
    suspend fun clear(userWalletId: UserWalletId, stakingIds: Set<StakingID>)
}