package com.tangem.domain.walletconnect.repository

import arrow.core.Either
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.flow.Flow

interface WcSessionsManager {
    val sessions: Flow<Map<UserWallet, List<WcSession>>>
    suspend fun saveSession(session: WcSession)
    suspend fun removeSession(session: WcSession): Either<Throwable, Unit>
    suspend fun findSessionByTopic(topic: String): WcSession?
}