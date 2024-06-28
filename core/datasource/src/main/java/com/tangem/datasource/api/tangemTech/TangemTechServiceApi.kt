package com.tangem.datasource.api.tangemTech

import com.tangem.datasource.config.models.ProviderModel
import retrofit2.http.GET

/**
 * Tangem Tech API for app services
 *
[REDACTED_AUTHOR]
 */
interface TangemTechServiceApi {

    @GET("networks/providers")
    suspend fun getBlockchainProviders(): Map<String, List<ProviderModel>>
}