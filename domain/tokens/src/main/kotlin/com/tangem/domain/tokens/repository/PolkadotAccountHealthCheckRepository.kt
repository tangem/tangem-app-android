package com.tangem.domain.tokens.repository

import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface PolkadotAccountHealthCheckRepository {

    suspend fun runCheck(userWalletId: UserWalletId, network: Network)

    fun subscribeToHasImmortalResults(): Flow<Pair<String, Boolean>>

    fun subscribeToHasResetResults(): Flow<Pair<String, Boolean>>
}