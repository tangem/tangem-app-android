package com.tangem.blockchainsdk.providers

import com.tangem.core.remote.ReadTimeout
import com.tangem.blockchainsdk.providers.models.ProviderModel
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

/**
 * Serves the blockchain RPC providers configuration. Owned by the Blockchain SDK, which is the sole
 * consumer of this endpoint; served from the shared Tangem gateway.
 */
interface BlockchainProvidersApi {

    @ReadTimeout(duration = 5, unit = TimeUnit.SECONDS)
    @GET("v1/networks/providers")
    suspend fun getBlockchainProviders(): Map<String, List<ProviderModel>>
}