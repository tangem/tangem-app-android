package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Repository for everything related to the blockchain networks
 * */
interface NetworksRepository {
    /**
     * Retrieves updates of network statuses of specified blockchain networks for a specific user wallet.
     *
     * Loads remote network statuses if they have expired.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param networks A set of network which statuses are to be retrieved.
     * @return A [Flow] emitting a set of [NetworkStatus] objects corresponding to the specified networks.
     */
    fun getNetworkStatusesUpdates(userWalletId: UserWalletId, networks: Set<Network>): Flow<Set<NetworkStatus>>

    /**
     * Fetches pending transactions for given network
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param networks A set of network which statuses are to be retrieved.
     */
    suspend fun fetchNetworkPendingTransactions(userWalletId: UserWalletId, networks: Set<Network>)

    /**
     * Retrieves network statuses of specified blockchain networks for a specific user wallet.
     *
     * Loads remote network statuses if they have expired or if [refresh] is `true`.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param networks A set of network which statuses are to be retrieved.
     * @param refresh A boolean flag indicating whether the data should be refreshed.
     * @return A [Flow] emitting a set of [NetworkStatus] objects corresponding to the specified networks.
     */
    suspend fun getNetworkStatusesSync(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        refresh: Boolean = false,
    ): Set<NetworkStatus>

    fun isNeedToCreateAccountWithoutReserve(network: Network): Boolean
}
