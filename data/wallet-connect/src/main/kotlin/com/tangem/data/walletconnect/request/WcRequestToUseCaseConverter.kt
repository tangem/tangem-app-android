package com.tangem.data.walletconnect.request

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.model.WcMethodName
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcMethodUseCase

interface WcRequestToUseCaseConverter {
    fun toWcMethodName(request: WcSdkSessionRequest): WcMethodName?
    suspend fun toUseCase(request: WcSdkSessionRequest): Either<HandleMethodError, WcMethodUseCase>

    /**
     * Answers [request] straight away when the session's own state already determines the answer, and
     * returns `true` once it has, so the request never reaches the user. Everything else returns `false`
     * and goes on to a confirmation screen as before.
     */
    suspend fun respondWithoutUi(request: WcSdkSessionRequest): Boolean = false

    companion object {
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