package com.tangem.datasource.api.gasless

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.gasless.models.GaslessServiceResponse
import com.tangem.datasource.api.gasless.models.GaslessSupportedTokens
import retrofit2.http.GET

interface GaslessTxServiceApi {

    @GET("api/v1/tokens")
    suspend fun getSupportedTokens(): ApiResponse<GaslessServiceResponse<GaslessSupportedTokens>>
}