package com.tangem.datasource.local.walletconnect

import com.tangem.domain.walletconnect.model.WcSessionDTO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface WalletConnectStore {

    val sessions: Flow<WcSessionCollection>
    suspend fun findSessionByTopic(topic: String) = sessions.first().find { it.topic == topic }

    suspend fun saveSessions(sessions: WcSessionCollection)
    suspend fun saveSession(session: WcSessionDTO) = saveSessions(setOf(session))

    suspend fun removeSessions(sessions: WcSessionCollection)
    suspend fun removeSession(session: WcSessionDTO) = removeSessions(setOf(session))
}