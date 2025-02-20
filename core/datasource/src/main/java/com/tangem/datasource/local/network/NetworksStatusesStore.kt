package com.tangem.datasource.local.network

import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface NetworksStatusesStore {

    fun get(key: UserWalletId): Flow<Set<NetworkStatus>>

    fun get(key: UserWalletId, networks: Set<Network>): Flow<Set<NetworkStatus>>

    suspend fun getSyncOrNull(key: UserWalletId): Set<NetworkStatus>?

    suspend fun store(key: UserWalletId, value: NetworkStatus)

    suspend fun storeAll(key: UserWalletId, values: Set<NetworkStatus>)

    suspend fun refresh(key: UserWalletId, networks: Set<Network>)
}