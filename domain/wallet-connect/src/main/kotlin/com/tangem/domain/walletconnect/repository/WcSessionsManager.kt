package com.tangem.domain.walletconnect.repository

import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface WcSessionsManager {
    val sessions: Flow<Map<UserWalletId, List<WcSession>>>
    suspend fun saveSessions(userWalletId: UserWalletId, session: WcSession)
    suspend fun removeSessions(userWalletId: UserWalletId, session: WcSession)
    suspend fun findSessionByTopic(topic: String): WcSession?
}
