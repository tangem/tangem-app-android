package com.tangem.network.api.tangemTech

import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.network.common.AddHeaderInterceptor
import com.tangem.network.common.CacheControlHttpInterceptor
import com.tangem.network.common.createRetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
[REDACTED_AUTHOR]
 */
class TangemTechService {
    private val headerInterceptors = mutableListOf<AddHeaderInterceptor>(
        CacheControlHttpInterceptor(cacheMaxAge)
    )
    
    private var api: TangemTechApi = createApi()

    suspend fun coins(
        contractAddress: String? = null,
        networkId: String? = null,
        active: Boolean? = null,
    ): Result<CoinsResponse> = withContext(Dispatchers.IO) {
        performRequest { api.coins(contractAddress, networkId, active) }
    }

    suspend fun rates(
        currency: String,
        ids: List<String>
    ): Result<RatesResponse> = withContext(Dispatchers.IO) {
        performRequest {
            api.rates(currency.lowercase(), ids.joinToString(","))
        }
    }

    suspend fun currencies(): Result<CurrenciesResponse> = withContext(Dispatchers.IO) {
        performRequest { api.currencies() }
    }

    fun addHeaderInterceptors(interceptors: List<AddHeaderInterceptor>) {
        headerInterceptors.removeAll(interceptors)
        headerInterceptors.addAll(interceptors)
        api = createApi()
    }

    private fun createApi(): TangemTechApi {
        val retrofit = createRetrofitInstance(
            baseUrl = baseUrl,
            interceptors = headerInterceptors.toList(),
//            logEnabled = true,
        )
        return retrofit.create(TangemTechApi::class.java)
    }

    companion object {
        const val baseUrl = "https://api.tangem-tech.com/v1/"
        const val cacheMaxAge = 600
    }
}