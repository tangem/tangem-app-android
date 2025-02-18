package com.tangem.datasource.local.token

import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/** Staking balance store */
interface StakingBalanceStore {

    /** Get flow of [YieldBalanceList] by [userWalletId] */
    fun get(userWalletId: UserWalletId): Flow<Set<YieldBalance>>

    /** Get flow of [YieldBalance] by [userWalletId], [address] and [integrationId] */
    fun get(userWalletId: UserWalletId, address: String, integrationId: String): Flow<YieldBalance?>

    /** Get [YieldBalanceList] synchronously or null by [userWalletId] */
    suspend fun getSyncOrNull(userWalletId: UserWalletId): Set<YieldBalance>?

    /** Get [YieldBalance] synchronously or null by [userWalletId], [address] and [integrationId] */
    suspend fun getSyncOrNull(userWalletId: UserWalletId, address: String, integrationId: String): YieldBalance?

    /** Store [items] by [userWalletId] */
    suspend fun store(userWalletId: UserWalletId, items: Set<YieldBalanceWrapperDTO>)

    /** Store [item] by [userWalletId], [integrationId] and [address] */
    suspend fun store(userWalletId: UserWalletId, integrationId: String, address: String, item: YieldBalanceWrapperDTO)
}