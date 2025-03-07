package com.tangem.tap.domain.walletconnect3

import com.reown.walletkit.client.Wallet
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.tangem.tap.domain.walletconnect2.domain.models.WalletConnectError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

interface WcRequestService {
    val requests: Flow<WcRequest<*>>
}

class DefaultWcRequestService(
    private val sessionsManager: WcSessionsManager,
    private val respondService: WcRespondService,
    private val requestAdapters: List<WcRequestHandler<WcMethod>>,
    private val scope: CoroutineScope, // todo(wc) is ok inject scope? featureScope like Decompose Model scope
) : WcRequestService, WcSdkObserver {

    override val requests: MutableSharedFlow<WcRequest<*>> = MutableSharedFlow()

    override fun onSessionRequest(
        sessionRequest: Wallet.Model.SessionRequest,
        verifyContext: Wallet.Model.VerifyContext,
    ) {
        // Triggered when a Dapp sends SessionRequest to sign a transaction or a message
        val method = sessionRequest.request.method
        val params = sessionRequest.request.params
        scope.launch {
            val session = sessionsManager.finsSessionByTopic(sessionRequest.topic)
            val handler: WcRequestHandler<WcMethod>? = requestAdapters.firstOrNull { it.canHandle(method) }

            val deserialized: WcMethod? = handler?.deserialize(method, params)
            if (handler == null || deserialized == null || session == null) {
                respondService.rejectRequest(sessionRequest, WalletConnectError.UnsupportedMethod.error)
                return@launch
            }

            val wcRequest = WcRequest(sessionRequest, session, deserialized)
            handler.handle(wcRequest)
            requests.emit(wcRequest)
        }
    }
}

// todo(wc) create our clone model?
typealias WcSdkSessionRequest = Wallet.Model.SessionRequest

data class WcRequest<M : WcMethod>(
    val rawSdkRequest: WcSdkSessionRequest,
    val session: WcSession,
    val method: M,
)

interface WcMethod {
    data object Unsupported : WcMethod
}

interface WcRequestHandler<M : WcMethod> {
    fun canHandle(methodName: String): Boolean
    fun deserialize(methodName: String, params: String): M?
    fun handle(wcRequest: WcRequest<M>)

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        inline fun <reified T> fromJson(params: String, moshi: Moshi): T? =
            runCatching { moshi.adapter<T>().fromJsonValue(params) }.getOrNull()
    }
}
