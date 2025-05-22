package com.tangem.data.networks.store

import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/** Store of [NetworkStatus]'es set */
internal interface NetworksStatusesStore {

    /** Get statuses by [userWalletId] */
    fun get(userWalletId: UserWalletId): Flow<Set<SimpleNetworkStatus>>

    /** Get status of [network] by [userWalletId] synchronously or null */
    suspend fun getSyncOrNull(userWalletId: UserWalletId, network: Network): SimpleNetworkStatus?

    /**
     * Update [source] of [network] by [userWalletId].
     * If the status is not found, create a new one by [ifNotFound].
     */
    suspend fun updateStatusSource(
        userWalletId: UserWalletId,
        network: Network,
        source: StatusSource,
        ifNotFound: (Network.ID) -> SimpleNetworkStatus? = { null },
    )

    /** Update [source] of [networks] by [userWalletId]. If the status is not found, create a new one by [ifNotFound] */
    suspend fun updateStatusSource(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        source: StatusSource,
        ifNotFound: (Network.ID) -> SimpleNetworkStatus? = { null },
    )

    /**
     * Store [status] by [userWalletId]. Rewrite the stored status with a new [status].
     *
     * See complex methods in `NetworksStatusesStoreExt`.
     */
    suspend fun store(userWalletId: UserWalletId, status: NetworkStatus)
}