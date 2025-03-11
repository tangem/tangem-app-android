package com.tangem.domain.tokens.repository

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.core.error.DataError
import com.tangem.domain.tokens.model.CryptoCurrencyAddress
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class MockNetworksRepository(
    private val statuses: Flow<Either<DataError, Set<NetworkStatus>>>,
) : NetworksRepository {

    override fun getNetworkStatusesUpdates(
        userWalletId: UserWalletId,
        networks: Set<Network>,
    ): Flow<Set<NetworkStatus>> {
        return statuses.map { it.getOrElse { e -> throw e } }
    }

    override suspend fun fetchNetworkStatuses(userWalletId: UserWalletId, networks: Set<Network>, refresh: Boolean) {
        /* no-op */
    }

    override fun getNetworkStatusesUpdatesLegacy(
        userWalletId: UserWalletId,
        networks: Set<Network>,
    ): Flow<Set<NetworkStatus>> {
        return statuses.map { it.getOrElse { e -> throw e } }
    }

    override suspend fun fetchNetworkPendingTransactions(userWalletId: UserWalletId, networks: Set<Network>) {
        // no-op
    }

    override suspend fun getNetworkStatusesSync(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        refresh: Boolean,
    ): Set<NetworkStatus> {
        return getNetworkStatusesUpdatesLegacy(userWalletId, networks).first()
    }

    override fun isNeedToCreateAccountWithoutReserve(network: Network) = false

    override suspend fun getNetworkAddresses(
        userWalletId: UserWalletId,
        network: Network,
    ): List<CryptoCurrencyAddress> {
        return emptyList()
    }
}