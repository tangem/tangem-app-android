package com.tangem.domain.dynamicaddresses.repository

import com.tangem.domain.dynamicaddresses.model.DynamicAddressesStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface DynamicAddressesRepository {

    fun getStatus(userWalletId: UserWalletId, network: Network): Flow<DynamicAddressesStatus>

    suspend fun enable(userWalletId: UserWalletId, network: Network, xpub: String)

    suspend fun disable(userWalletId: UserWalletId, network: Network)

    suspend fun getReceiveAddress(userWalletId: UserWalletId, network: Network): String

    // for explorer url
    suspend fun getLastUsedReceiveAddress(userWalletId: UserWalletId, network: Network): String?

    suspend fun hasNonBaseBalances(userWalletId: UserWalletId, network: Network): Boolean
}