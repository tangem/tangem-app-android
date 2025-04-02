package com.tangem.data.walletconnect.respond

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.respond.WcRespondService
import kotlinx.coroutines.suspendCancellableCoroutine
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
                    continuation.resume(Unit.right())
                },
                onError = {
                    continuation.resume(it.throwable.left())
                },
            )
        }

    override suspend fun rejectRequest(request: WcSdkSessionRequest, message: String) =
        suspendCancellableCoroutine { continuation ->
            WalletKit.respondSessionRequest(
                params = Wallet.Params.SessionRequestResponse(
                    sessionTopic = request.topic,
                    jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcError(
                        id = request.request.id,
                        code = 0,
                        message = message,
                    ),
                ),
                onSuccess = {
                    continuation.resume(Unit.right())
                },
                onError = {
                    continuation.resume(it.throwable.left())
                },
            )
        }
}