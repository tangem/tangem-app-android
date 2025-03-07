package com.tangem.tap.domain.walletconnect3

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first

interface WcRespondService {
    suspend fun respond(request: WcSdkSessionRequest, response: String): Either<Throwable, Unit>
    suspend fun rejectRequest(request: WcSdkSessionRequest, message: String = ""): Either<Throwable, Unit>
}

class DefaultWcRespondService : WcRespondService {

    override suspend fun respond(request: WcSdkSessionRequest, response: String): Either<Throwable, Unit> =
        callbackFlow {
            WalletKit.respondSessionRequest(
                params = Wallet.Params.SessionRequestResponse(
                    sessionTopic = request.topic,
                    jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(
                        id = request.request.id,
                        result = response,
                    ),
                ),
                onSuccess = {
                    channel.trySend(Unit.right())
                    channel.close()
                },
                onError = {
                    channel.trySend(it.throwable.left())
                    channel.close()
                },
            )
            awaitClose()
        }.first()

    override suspend fun rejectRequest(request: WcSdkSessionRequest, message: String) = callbackFlow {
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
                channel.trySend(Unit.right())
                channel.close()
            },
            onError = {
                channel.trySend(it.throwable.left())
                channel.close()
            },
        )
        awaitClose()
    }.first()
}
