package com.tangem.domain.walletconnect.usecase

import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class WcSessionsUseCase(private val sessionsManager: WcSessionsManager) {
    operator fun invoke(): Flow<Map<UserWalletId, List<WcSession>>> {
        return sessionsManager.sessions
    }

    suspend fun invokeSync(): Map<UserWalletId, List<WcSession>> {
        return sessionsManager.sessions.first()
    }
}