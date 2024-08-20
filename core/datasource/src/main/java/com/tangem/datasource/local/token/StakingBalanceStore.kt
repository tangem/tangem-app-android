package com.tangem.datasource.local.token

import com.tangem.datasource.api.stakekit.models.response.model.BalanceDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface StakingBalanceStore {

    fun get(userWalletId: UserWalletId): Flow<Set<YieldBalanceWrapperDTO>>

    suspend fun getSyncOrNull(userWalletId: UserWalletId): Set<YieldBalanceWrapperDTO>?

    suspend fun store(userWalletId: UserWalletId, items: Set<YieldBalanceWrapperDTO>)

    fun get(userWalletId: UserWalletId, integrationId: String): Flow<List<BalanceDTO>>

    suspend fun getSyncOrNull(userWalletId: UserWalletId, integrationId: String): List<BalanceDTO>?

    suspend fun store(userWalletId: UserWalletId, integrationId: String, item: YieldBalanceWrapperDTO)
}
