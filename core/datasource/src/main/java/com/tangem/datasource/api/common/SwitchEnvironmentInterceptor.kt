package com.tangem.datasource.api.common

import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.utils.ProviderSuspend
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.IOException

/**
 * Switch api environment [Interceptor]
 *
 * @property id                api config id [ApiConfig.ID]
 * @property apiConfigsManager api configs manager
 *
[REDACTED_AUTHOR]
 */
internal class SwitchEnvironmentInterceptor(
    private val id: ApiConfig.ID,
    private val apiConfigsManager: ApiConfigsManager,
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val builder = request.newBuilder()

        val environmentConfig = apiConfigsManager.getEnvironmentConfig(id)

        request = builder
            .url(url = request.url.adjustBaseUrl(environmentConfig.baseUrl))
            .addHeaders(headers = environmentConfig.headers)
            .build()

        return chain.proceed(request)
    }

    private fun HttpUrl.adjustBaseUrl(url: String): HttpUrl {
        return this.newBuilder()
            .host(host = url.toHttpUrl().host)
            .build()
    }

    private fun Request.Builder.addHeaders(headers: Map<String, ProviderSuspend<String>>): Request.Builder {
        runBlocking {
            headers.forEach { (name, valueProvider) ->
                val value = valueProvider()

                if (value.isNotBlank()) addHeader(name = name, value = value)
            }
        }

        return this
    }
}