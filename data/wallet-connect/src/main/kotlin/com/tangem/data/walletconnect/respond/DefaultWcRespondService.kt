package com.tangem.data.walletconnect.respond

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.reown.walletkit.client.Wallet
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toHexString
import com.tangem.data.walletconnect.sdk.WcSdk
import com.tangem.domain.walletconnect.WC_TAG
import com.tangem.domain.walletconnect.model.WcRequestError
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.joda.time.Duration

internal class DefaultWcRespondService(
    private val wcSdk: WcSdk,
) : WcRespondService {

    internal val expireDuration = Duration.standardSeconds(120)
    internal val cachedRequest = MutableStateFlow<Set<Pair<Long, String>>>(emptySet())

    internal fun sessionRequestHash(request: WcSdkSessionRequest): String {
        return request.request.params.calculateSha256().toHexString()
    }

    override suspend fun respond(request: WcSdkSessionRequest, response: String): Either<WcRequestError, String> {
        val params = Wallet.Params.SessionRequestResponse(
            sessionTopic = request.topic,
            jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(
                id = request.request.id,
                result = response,
            ),
        )

        return wcSdk.respondSessionRequest(params).fold(
            ifLeft = { error ->
                TangemLogger.withTag(WC_TAG).e("Failed respond for request $request", error)
                WcRequestError.UnknownError(error).left()
            },
            ifRight = { requestResponse ->
                when (val jsonRpcResponse = requestResponse.jsonRpcResponse) {
                    is Wallet.Model.JsonRpcResponse.JsonRpcError -> {
                        TangemLogger.withTag(WC_TAG).e("Failed respond $jsonRpcResponse for request $request")
                        WcRequestError.WcRespondError(
                            code = jsonRpcResponse.code,
                            message = jsonRpcResponse.message,
                        ).left()
                    }
                    is Wallet.Model.JsonRpcResponse.JsonRpcResult -> {
                        TangemLogger.withTag(WC_TAG).i("Successful respond $jsonRpcResponse for request $request")
                        if (jsonRpcResponse.result == null) {
                            TangemLogger.withTag(WC_TAG).e(
                                "Response result is null, but requestResponse should be String. Casted to empty",
                            )
                        }
                        jsonRpcResponse.result.orEmpty().right()
                    }
                }
            },
        )
    }

    override fun isRequestActual(request: WcSdkSessionRequest): Boolean {
        val requestId = request.request.id

        // SDK read failures are intentionally NOT caught here: they propagate so the sign flow surfaces
        // a retriable error instead of definitively rejecting a request that may still be valid. Only a
        // successfully-read state showing no session / no pending request means "not actual".
        if (wcSdk.activeSessionByTopic(request.topic) == null) {
            TangemLogger.withTag(WC_TAG).i(
                "Request is not actual, no active session: topic=${request.topic} id=$requestId",
            )
            return false
        }

        val isStillPending = wcSdk.pendingSessionRequests(request.topic)
            .any { pending -> pending.request.id == requestId }
        if (!isStillPending) {
            TangemLogger.withTag(WC_TAG).i(
                "Request is not actual, no longer pending: topic=${request.topic} id=$requestId",
            )
        }
        return isStillPending
    }

    override fun rejectRequestNonBlock(request: WcSdkSessionRequest, message: String) {
        TangemLogger.withTag(WC_TAG).i("reject request $request")
        removeCachedRequest(request)
        wcSdk.respondSessionRequestNonBlocking(
            params = Wallet.Params.SessionRequestResponse(
                sessionTopic = request.topic,
                jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcError(
                    id = request.request.id,
                    code = 0,
                    message = message,
                ),
            ),
        )
    }

    private fun removeCachedRequest(request: WcSdkSessionRequest) {
        cachedRequest.update { set ->
            set.filterTo(mutableSetOf()) { (_, hash) -> hash != sessionRequestHash(request) }
        }
    }
}