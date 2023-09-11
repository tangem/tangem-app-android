package com.tangem.domain.tokens.repository

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.core.error.DataError
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class MockNetworksRepository(
    private val networks: Either<DataError, Set<Network>>,
    private val statuses: Flow<Either<DataError, Set<NetworkStatus>>>,
) : NetworksRepository {

    override fun getNetworks(networksIds: Set<Network.ID>): Set<Network> {
        return networks.getOrElse { throw it }
    }

    override fun getNetworkStatusesUpdates(
        userWalletId: UserWalletId,
        networks: Set<Network.ID>,
    ): Flow<Set<NetworkStatus>> {
        return statuses.map { it.getOrElse { e -> throw e } }
    }

    override suspend fun getNetworkStatusesSync(
        userWalletId: UserWalletId,
        networks: Set<Network.ID>,
        refresh: Boolean,
    ): Set<NetworkStatus> {
        return getNetworkStatusesUpdates(userWalletId, networks).first()
    }
}
