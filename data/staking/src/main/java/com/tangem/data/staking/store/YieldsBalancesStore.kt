package com.tangem.data.staking.store

import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Store of [YieldBalance]'s set
 *
[REDACTED_AUTHOR]
 */
interface YieldsBalancesStore {

    /** Get flow of [YieldBalance]'s set by [userWalletId] */
    fun get(userWalletId: UserWalletId): Flow<Set<YieldBalance>>

    /** Get [YieldBalance] by [userWalletId] and [stakingId] synchronously or null */
    suspend fun getSyncOrNull(userWalletId: UserWalletId, stakingId: StakingID): YieldBalance?

    /**  Get all [YieldBalance] by [userWalletId] synchronously or null */
    suspend fun getAllSyncOrNull(userWalletId: UserWalletId): Set<YieldBalance>?

    /** Refresh balance of [stakingId] by [userWalletId] */
    suspend fun refresh(userWalletId: UserWalletId, stakingId: StakingID)

    /** Refresh balances of [stakingIds] by [userWalletId] */
    suspend fun refresh(userWalletId: UserWalletId, stakingIds: Set<StakingID>)

    /** Store actual [values] by [userWalletId] */
    suspend fun storeActual(userWalletId: UserWalletId, values: Set<YieldBalanceWrapperDTO>)

    /** Store error by [userWalletId] and [stakingIds] */
    suspend fun storeError(userWalletId: UserWalletId, stakingIds: Set<StakingID>)
}