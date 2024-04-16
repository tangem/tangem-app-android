package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface PolkadotAccountHealthCheckRepository {

    suspend fun runCheck(userWalletId: UserWalletId, network: Network)

    fun subscribeToHasImmortalResults(): Flow<Pair<String, Boolean>>

    fun subscribeToHasResetResults(): Flow<Pair<String, Boolean>>
}