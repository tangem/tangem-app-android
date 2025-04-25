package com.tangem.domain.walletconnect.usecase.disconnect

import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.toList

class WcDisconnectUseCase(
    private val sessionsManager: WcSessionsManager,
) {

    suspend fun disconnectAll() {
        sessionsManager.sessions.first()
            .flatMap { it.value }
            .map { session -> flow { emit(disconnect(session)) } }
            .merge()
            .toList()
    }

    suspend fun disconnect(topic: String) {
        val session = sessionsManager.findSessionByTopic(topic) ?: return
        disconnect(session)
    }

    suspend fun disconnect(session: WcSession) {
        sessionsManager.removeSession(session.userWalletId, session)
    }
}