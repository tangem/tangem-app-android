package com.tangem.datasource.api.tangemTech

import com.tangem.datasource.config.models.ProviderModel
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * Tangem Tech API for app services
 *
* [REDACTED_AUTHOR]
 */
interface TangemTechServiceApi {

    @GET("networks/providers")
    suspend fun getBlockchainProviders(
        @Header("card_public_key") cardPublicKey: String,
        @Header("card_id") cardId: String,
    ): Map<String, List<ProviderModel>>
}
