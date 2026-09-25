package com.tangem.data.walletconnect.request

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.model.WcMethodName
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcMethodUseCase

interface WcRequestToUseCaseConverter {
    fun toWcMethodName(request: WcSdkSessionRequest): WcMethodName?
    suspend fun toUseCase(request: WcSdkSessionRequest): Either<HandleMethodError, WcMethodUseCase>

    companion object {

        /**
         * `true` when the request's CAIP-2 chain id belongs to [namespaceKey] (or carries no chain id at all).
         * Method names are namespace-specific (`eth_*`, `solana_*`, …); dispatching on the name alone would let
         * an `eth_sendTransaction` sent on a `solana:` chain reach the EVM handler.
         */
        fun WcSdkSessionRequest.isInNamespace(namespaceKey: String): Boolean {
            val chainId = this.chainId ?: return true
            return CAIP2.fromRaw(chainId)?.namespace == namespaceKey
        }

        @OptIn(ExperimentalStdlibApi::class)
        inline fun <reified T> Moshi.fromJson(params: String): Either<Throwable, T?> {
            return runCatching { this.adapter<T>().fromJson(params) }
                .fold(
                    onSuccess = { it.right() },
                    onFailure = { it.left() },
                )
        }
    }
}