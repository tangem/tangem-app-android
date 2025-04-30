package com.tangem.data.walletconnect.request

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.tangem.domain.walletconnect.model.WcMethodName
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcMethodUseCase

interface WcRequestToUseCaseConverter {
    fun toWcMethodName(request: WcSdkSessionRequest): WcMethodName?
    suspend fun toUseCase(request: WcSdkSessionRequest): WcMethodUseCase?

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        inline fun <reified T> Moshi.fromJson(params: String): T? =
            runCatching { this.adapter<T>().fromJson(params) }.getOrNull()
    }
}