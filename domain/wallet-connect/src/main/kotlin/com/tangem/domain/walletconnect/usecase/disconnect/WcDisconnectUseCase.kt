package com.tangem.domain.walletconnect.usecase.disconnect

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.walletconnect.WcAnalyticEvents
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.toList

class WcDisconnectUseCase(
    private val sessionsManager: WcSessionsManager,
    private val analytics: AnalyticsEventHandler,
) {

    suspend fun disconnectAll() {
        analytics.send(WcAnalyticEvents.ButtonDisconnectAll)
        sessionsManager.sessions.first()
            .flatMap { it.value }
            .map { session -> flow { emit(internalDisconnect(session)) } }
            .merge()
            .toList()
    }

    suspend fun disconnect(topic: String) {
        val session = sessionsManager.findSessionByTopic(topic) ?: return
        analytics.send(WcAnalyticEvents.ButtonDisconnect(session))
        internalDisconnect(session)
    }

    suspend fun disconnect(session: WcSession) {
        analytics.send(WcAnalyticEvents.ButtonDisconnect(session))
        internalDisconnect(session)
    }

    private suspend fun internalDisconnect(session: WcSession) {
        sessionsManager.removeSession(session)
    }
}