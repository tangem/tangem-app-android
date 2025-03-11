package com.tangem.datasource.local.token

import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/** Staking balance store */
interface StakingBalanceStore {

    /** Get flow of [YieldBalanceList] by [userWalletId] and [stakingIds] */
    fun get(userWalletId: UserWalletId, stakingIds: List<StakingID>): Flow<Set<YieldBalance>>

    /** Get flow of [YieldBalance] by [userWalletId] and [stakingID] */
    fun get(userWalletId: UserWalletId, stakingID: StakingID): Flow<YieldBalance?>

    /** Get all [YieldBalance] synchronously or null by [userWalletId] */
    suspend fun getSyncOrNull(userWalletId: UserWalletId): Set<YieldBalance>?

    /** Get [YieldBalanceList] synchronously or null by [userWalletId] and [stakingIds] */
    suspend fun getSyncOrNull(userWalletId: UserWalletId, stakingIds: List<StakingID>): Set<YieldBalance>?

    /** Get [YieldBalance] synchronously or null by [userWalletId] and [stakingID] */
    suspend fun getSyncOrNull(userWalletId: UserWalletId, stakingID: StakingID): YieldBalance?

    /** Store [items] by [userWalletId] */
    suspend fun store(userWalletId: UserWalletId, items: Set<YieldBalanceWrapperDTO>)

    /** Store [item] by [userWalletId] and [stakingID] */
    suspend fun store(userWalletId: UserWalletId, stakingID: StakingID, item: YieldBalanceWrapperDTO)

    /** Store [item] by [userWalletId] */
    suspend fun storeSingleYieldBalance(userWalletId: UserWalletId, item: YieldBalance)

    /** Refresh balances of [stakingIds] by [userWalletId] */
    suspend fun refresh(userWalletId: UserWalletId, stakingIds: List<StakingID>)

    data class StakingID(val integrationId: String, val address: String)
}