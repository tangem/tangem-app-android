package com.tangem.network.api.tangemTech

import com.tangem.common.services.Result
import com.tangem.network.common.AddHeaderInterceptor
import com.tangem.network.common.CacheHttpInterceptor
import com.tangem.network.common.createRetrofitInstance

/**
[REDACTED_AUTHOR]
 */
class TangemTechService {

    private val headerInterceptors = mutableListOf<AddHeaderInterceptor>(
        CacheHttpInterceptor(cacheMaxAge)
    )

    private var api: TangemTechApi = createApi()

    suspend fun coinsPrices(
        currency: String,
        ids: List<String>
    ): Result<CoinsPricesResponse> {
        return api.coinsPrices(currency, ids)
    }

    suspend fun coinsCheckAddress(
        contractAddress: String,
        networkId: String? = null
    ): Result<CoinsCheckAddressResponse> {
        return api.coinsCheckAddress(contractAddress, networkId)
    }

    suspend fun coinsCurrencies(): Result<CoinsCurrenciesResponse> {
        return api.coinsCurrencies()
    }

    suspend fun coinsTokens(): Result<CoinsCurrenciesResponse> {
        return api.coinsTokens()
    }

    fun addHeaderInterceptors(interceptors: List<AddHeaderInterceptor>) {
        headerInterceptors.removeAll(interceptors)
        headerInterceptors.addAll(interceptors)
        api = createApi()
    }

    private fun createApi(): TangemTechApi {
        val retrofit = createRetrofitInstance(
            baseUrl = baseUrl,
            interceptors = headerInterceptors.toList()
        )

        return retrofit.create(TangemTechApi::class.java)
    }

    companion object {
        const val baseUrl = "https://api.tangem-tech.com/"
        const val cacheMaxAge = 600
    }
}

class TangemAuthInterceptor(
    private val cardPublicKeyHex: String
) : AddHeaderInterceptor(
    mapOf("card_public_key" to cardPublicKeyHex)
)