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
 * @property baseUrls          base urls for all api config environments
 * @property apiConfigsManager api configs manager
 *
[REDACTED_AUTHOR]
 */
internal class SwitchEnvironmentInterceptor(
    private val id: ApiConfig.ID,
    private val baseUrls: Set<String>,
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

    private fun HttpUrl.adjustBaseUrl(newBaseUrl: String): HttpUrl {
        val currentUrl = this.toString()
        val currentBaseUrl = baseUrls.first { currentUrl.contains(it) }

        return currentUrl
            .replace(oldValue = currentBaseUrl, newValue = newBaseUrl)
            .toHttpUrl()
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