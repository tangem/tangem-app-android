package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Repository for everything related to the blockchain networks
 * */
interface NetworksRepository {

    fun getNetworks(networksIds: Set<Network.ID>): Set<Network>

    fun getNetworkStatuses(
        userWalletId: UserWalletId,
        networks: Map<Network.ID, Set<CryptoCurrency.ID>>,
        refresh: Boolean,
    ): Flow<Set<NetworkStatus>>
}