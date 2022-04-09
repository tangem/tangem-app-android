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
        return try {
            api.coinsPrices(currency, ids)
        } catch (ex: Exception) {
            Result.Failure(ex)
        }
    }

    suspend fun coinsCheckAddress(
        contractAddress: String,
        networkId: String? = null
    ): Result<CoinsCheckAddressResponse> {
        return try {
            api.coinsCheckAddress(contractAddress, networkId)
        } catch (ex: Exception) {
            Result.Failure(ex)
        }
    }

    suspend fun coinsCurrencies(): Result<CoinsCurrenciesResponse> {
        return try {
            api.coinsCurrencies()
        } catch (ex: Exception) {
            Result.Failure(ex)
        }
    }

    suspend fun coinsTokens(): Result<CoinsCurrenciesResponse> {
        return try {
            api.coinsTokens()
        } catch (ex: Exception) {
            Result.Failure(ex)
        }
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