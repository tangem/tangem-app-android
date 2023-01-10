package com.tangem.datasource.api.tangemTech

import com.tangem.datasource.api.common.AddHeaderInterceptor
import com.tangem.datasource.api.common.CacheControlHttpInterceptor
import com.tangem.datasource.api.common.createRetrofitInstance

/**
[REDACTED_AUTHOR]
 */
class TangemTechService(
    private val logIsEnabled: Boolean = false,
) {

    private val headerInterceptors = mutableListOf<AddHeaderInterceptor>(
        CacheControlHttpInterceptor(cacheMaxAge),
    )

    var api: TangemTechApi = createApi()

    fun addHeaderInterceptors(interceptors: List<AddHeaderInterceptor>) {
        headerInterceptors.removeAll(interceptors)
        headerInterceptors.addAll(interceptors)
        api = createApi()
    }

    private fun createApi(): TangemTechApi {
        val retrofit = createRetrofitInstance(
            baseUrl = baseUrl,
            interceptors = headerInterceptors.toList(),
            logEnabled = logIsEnabled,
        )
        return retrofit.create(TangemTechApi::class.java)
    }

    companion object {
        const val baseUrl = "https://api.tangem-tech.com/v1/"
        const val cacheMaxAge = 600
    }
}