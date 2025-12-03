package com.tangem.data.walletconnect.respond

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toHexString
import com.tangem.data.walletconnect.utils.WC_TAG
import com.tangem.domain.walletconnect.model.WcRequestError
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import org.joda.time.Duration
import timber.log.Timber
import kotlin.coroutines.resume

internal class DefaultWcRespondService : WcRespondService {

    internal val expireDuration = Duration.standardSeconds(120)
    internal val cachedRequest = MutableStateFlow<Set<Pair<Long, String>>>(setOf())

    internal fun sessionRequestHash(request: WcSdkSessionRequest): String {
        return request.request.params.calculateSha256().toHexString()
    }

    override suspend fun respond(request: WcSdkSessionRequest, response: String): Either<WcRequestError, String> =
        suspendCancellableCoroutine { continuation ->
            WalletKit.respondSessionRequest(
                params = Wallet.Params.SessionRequestResponse(
                    sessionTopic = request.topic,
                    jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(
                        id = request.request.id,
                        result = response,
                    ),
                ),
                onSuccess = {
                    if (continuation.isCompleted) return@respondSessionRequest
                    val result = when (val response = it.jsonRpcResponse) {
                        is Wallet.Model.JsonRpcResponse.JsonRpcError -> {
                            Timber.tag(WC_TAG).e("Failed respond $response for request $request")
                            WcRequestError.WcRespondError(
                                code = response.code,
                                message = response.message,
                            ).left()
                        }
                        is Wallet.Model.JsonRpcResponse.JsonRpcResult -> {
                            Timber.tag(WC_TAG).i("Successful respond $response for request $request")
                            if (response.result == null) {
                                Timber.tag(WC_TAG).e(
                                    "Response result is null, but it should be String. Casted to empty",
                                )
                            }
                            (response.result ?: "").right()
                        }
                    }
                    continuation.resume(result)
                },
                onError = {
                    if (continuation.isCompleted) return@respondSessionRequest
                    Timber.tag(WC_TAG).e(it.throwable, "Failed respond for request $request")
                    continuation.resume(WcRequestError.UnknownError(it.throwable).left())
                },
            )
        }

    override fun rejectRequestNonBlock(request: WcSdkSessionRequest, message: String) {
        Timber.tag(WC_TAG).i("reject request $request")
        removeCachedRequest(request)
        WalletKit.respondSessionRequest(
            params = Wallet.Params.SessionRequestResponse(
                sessionTopic = request.topic,
                jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcError(
                    id = request.request.id,
                    code = 0,
                    message = message,
                ),
            ),
            onSuccess = {},
            onError = {},
        )
    }

    private fun removeCachedRequest(request: WcSdkSessionRequest) {
        cachedRequest.update { set ->
            set.filterTo(mutableSetOf()) { (_, hash) -> hash != sessionRequestHash(request) }
        }
    }
}