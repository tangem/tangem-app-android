package com.tangem.datasource.api.tangemTech

import com.tangem.datasource.config.models.ProviderModel
import retrofit2.http.GET

/**
 * Tangem Tech API for app services
 *
 * @author Andrew Khokhlov on 15/04/2024
 */
interface TangemTechServiceApi {

    @GET("networks/providers")
    suspend fun getBlockchainProviders(): Map<String, List<ProviderModel>>
}
