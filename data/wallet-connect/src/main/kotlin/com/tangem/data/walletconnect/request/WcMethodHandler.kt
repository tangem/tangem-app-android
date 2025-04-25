package com.tangem.data.walletconnect.request

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.tangem.domain.walletconnect.model.WcMethod
import com.tangem.domain.walletconnect.model.WcRequest

interface WcMethodHandler<out M : WcMethod> {
    fun canHandle(methodName: String): Boolean
    fun deserialize(methodName: String, params: String): M?
    fun handle(wcRequest: WcRequest<WcMethod>)

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        inline fun <reified T> fromJson(params: String, moshi: Moshi): T? =
            runCatching { moshi.adapter<T>().fromJsonValue(params) }.getOrNull()
    }
}