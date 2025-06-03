package com.tangem.data.walletconnect.respond

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.tangem.data.walletconnect.utils.WC_TAG
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

internal class DefaultWcRespondService : WcRespondService {

    override suspend fun respond(request: WcSdkSessionRequest, response: String): Either<Throwable, Unit> =
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
                    Timber.tag(WC_TAG).i("Successful respond for request $request")
                    continuation.resume(Unit.right())
                },
                onError = {
                    Timber.tag(WC_TAG).e(it.throwable, "Failed respond for request $request")
                    continuation.resume(it.throwable.left())
                },
            )
        }

    override fun rejectRequestNonBlock(request: WcSdkSessionRequest, message: String) {
        Timber.tag(WC_TAG).i("reject request $request")
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
}
