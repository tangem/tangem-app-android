package com.tangem.data.walletconnect.pair

import com.tangem.domain.walletconnect.WcPairService
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

class DefaultWcPairService @Inject constructor(
    private val sessionsManager: WcSessionsManager,
) : WcPairService {
    private val _pairFlow: Channel<WcPairRequest> = Channel(Channel.BUFFERED)
    override val pairFlow: Flow<WcPairRequest> = _pairFlow
        .receiveAsFlow()
        .filter(::filterDeeplinkRequestWithSessionTopic)

    override fun pair(request: WcPairRequest) {
        _pairFlow.trySend(request)
    }

    // some dApp sends deeplink with session request
    // we filter session exist, but start dApp pair flow if unexist
    private suspend fun filterDeeplinkRequestWithSessionTopic(request: WcPairRequest): Boolean {
        when (request.source) {
            WcPairRequest.Source.QR,
            WcPairRequest.Source.CLIPBOARD,
            WcPairRequest.Source.ETC,
            -> return true
            WcPairRequest.Source.DEEPLINK -> Unit
        }

        val isExistSession = existSessionTopic(request.uri).getOrNull() ?: false
        return !isExistSession
    }

    private suspend fun existSessionTopic(uri: String) = runCatching {
        val deeplinkRegex = Regex(WC_PARAM_REGEX)
        val matched = deeplinkRegex.findAll(uri)
        val sessionTopic = matched
            .firstOrNull { it.value.contains(WC_TOPIC_QUERY_NAME) }
            ?.groupValues
            ?.lastOrNull()
            ?: return@runCatching false
        val isExistSession = sessionsManager.findSessionByTopic(sessionTopic) != null
        return@runCatching isExistSession
    }

    private companion object {
        const val WC_TOPIC_QUERY_NAME = "sessionTopic"
        const val WC_PARAM_REGEX = "([a-zA-Z\\d-]+)=([a-zA-Z\\d]+)"
    }
}