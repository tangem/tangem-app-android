package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Repository for everything related to the blockchain networks
 * */
interface NetworksRepository {

    /**
     * Retrieves the details of the specified blockchain networks, identified by their unique IDs.
     *
     * @param networksIds The unique identifiers of the networks to be retrieved.
     * @return A set of [Network] objects corresponding to the specified network IDs.
     */
    fun getNetworks(networksIds: Set<Network.ID>): Set<Network>

    /**
     * Retrieves updates of network statuses of specified blockchain networks for a specific user wallet.
     *
     * Loads remote network statuses if they have expired.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param networks A set of network IDs which statuses are to be retrieved.
     * @return A [Flow] emitting a set of [NetworkStatus] objects corresponding to the specified networks.
     */
    fun getNetworkStatusesUpdates(userWalletId: UserWalletId, networks: Set<Network.ID>): Flow<Set<NetworkStatus>>

    /**
     * Retrieves network statuses of specified blockchain networks for a specific user wallet.
     *
     * Loads remote network statuses if they have expired or if [refresh] is `true`.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param networks A set of network IDs which statuses are to be retrieved.
     * @param refresh A boolean flag indicating whether the data should be refreshed.
     * @return A [Flow] emitting a set of [NetworkStatus] objects corresponding to the specified networks.
     */
    suspend fun getNetworkStatusesSync(
        userWalletId: UserWalletId,
        networks: Set<Network.ID>,
        refresh: Boolean,
    ): Set<NetworkStatus>
}
