package com.tangem.datasource.api.common

import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import okio.IOException

/**
 * Switch base url [Interceptor]
 *
 * @property id                api config id [ApiConfig.ID]
 * @property apiConfigsManager api configs manager
 *
[REDACTED_AUTHOR]
 */
internal class SwitchBaseUrlInterceptor(
    private val id: ApiConfig.ID,
    private val apiConfigsManager: ApiConfigsManager,
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val builder = request.newBuilder()

        request = builder
            .url(url = request.url.adjustBaseUrl())
            .build()

        return chain.proceed(request)
    }

    private fun HttpUrl.adjustBaseUrl(): HttpUrl {
        val host = apiConfigsManager.getBaseUrl(id).toHttpUrl().host

        return this.newBuilder()
            .host(host)
            .build()
    }
}