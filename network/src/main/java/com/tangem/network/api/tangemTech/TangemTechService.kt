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

    val coins: CoinsRoute = CoinsRoute()

    private val techRoutes: List<TangemTechRoute> = listOf(
        coins
    )

    private val headerInterceptors = mutableListOf<AddHeaderInterceptor>(
        CacheControlHttpInterceptor(cacheMaxAge)
    )

    private var api: TangemTechApi = createApi()

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
        return retrofit.create(TangemTechApi::class.java).apply {
            techRoutes.forEach { it.setApi(this) }
        }
    }

    companion object {
        const val baseUrl = "https://api.tangem-tech.com/"
        const val cacheMaxAge = 600
    }
}

private interface TangemTechRoute {
    fun setApi(api: TangemTechApi)
}

class CoinsRoute : TangemTechRoute {
    private lateinit var api: TangemTechApi

    override fun setApi(api: TangemTechApi) {
        this.api = api
    }

    suspend fun prices(
        currency: String,
        ids: List<String>
    ): Result<Coins.PricesResponse> = withContext(Dispatchers.IO) {
        performRequest {
            api.coinsPrices(currency.lowercase(), ids.joinToString(","))
        }
    }

    suspend fun checkAddress(
        contractAddress: String,
        networkId: String? = null
    ): Result<Coins.CheckAddressResponse> = withContext(Dispatchers.IO) {
        performRequest { api.coinsCheckAddress(contractAddress, networkId) }
    }

    suspend fun currencies(): Result<Coins.CurrenciesResponse> = withContext(Dispatchers.IO) {
        performRequest { api.coinsCurrencies() }
    }

    suspend fun tokens(): Result<Coins.TokensResponse> = withContext(Dispatchers.IO) {
        performRequest { api.coinsTokens() }
    }
}