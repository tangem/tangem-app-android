package com.tangem.data.walletconnect.request

import com.reown.walletkit.client.Wallet
import com.tangem.data.walletconnect.utils.WcSdkObserver
import com.tangem.data.walletconnect.utils.WcSdkSessionRequestConverter
import com.tangem.domain.walletconnect.model.WcMethod
import com.tangem.domain.walletconnect.model.WcRequest
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.walletconnect.request.WcRequestService
import com.tangem.domain.walletconnect.respond.WcRespondService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

internal class DefaultWcRequestService(
    private val sessionsManager: WcSessionsManager,
    private val respondService: WcRespondService,
    private val requestAdapters: Set<WcMethodHandler<WcMethod>>,
    private val scope: CoroutineScope,
) : WcRequestService, WcSdkObserver {

    override val requests: MutableSharedFlow<WcRequest<*>> = MutableSharedFlow()

    override fun onSessionRequest(
        sessionRequest: Wallet.Model.SessionRequest,
        verifyContext: Wallet.Model.VerifyContext,
    ) {
        // Triggered when a Dapp sends SessionRequest to sign a transaction or a message
        val sr = WcSdkSessionRequestConverter.convert(sessionRequest)
        val method = sr.request.method
        val params = sr.request.params
        scope.launch {
            val session = sessionsManager.findSessionByTopic(sr.topic)
            val handler: WcMethodHandler<WcMethod>? = requestAdapters.firstOrNull { it.canHandle(method) }

            val deserialized: WcMethod? = handler?.deserialize(method, params)
            if (handler == null || deserialized == null || session == null) {
                respondService.rejectRequest(sr, "UnsupportedMethod") // todo(wc) use our domain error
                return@launch
            }

            val wcRequest = WcRequest(sr, session, deserialized)
            handler.handle(wcRequest)
            requests.emit(wcRequest)
        }
    }
}