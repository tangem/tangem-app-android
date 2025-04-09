package com.tangem.data.networks.store

import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/** Store of [NetworkStatus]'es set */
internal interface NetworksStatusesStoreV2 {

    /** Get statuses by [userWalletId] */
    fun get(userWalletId: UserWalletId): Flow<Set<SimpleNetworkStatus>>

    /** Refresh status of [network] by [userWalletId] */
    suspend fun refresh(userWalletId: UserWalletId, network: Network)

    /** Refresh statuses of [networks] by [userWalletId] */
    suspend fun refresh(userWalletId: UserWalletId, networks: Set<Network>)

    /** Store actual [NetworkStatus] by [userWalletId] */
    suspend fun storeActual(userWalletId: UserWalletId, value: NetworkStatus)

    /** Store error by [userWalletId] and [network]  */
    suspend fun storeError(userWalletId: UserWalletId, network: Network)
}