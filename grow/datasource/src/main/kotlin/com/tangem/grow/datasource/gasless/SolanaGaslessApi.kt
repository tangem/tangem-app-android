package com.tangem.grow.datasource.gasless

import com.tangem.core.remote.ReadTimeout
import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.gasless.models.GaslessServiceResponse
import com.tangem.grow.datasource.gasless.models.solana.SolanaFeePayerResponse
import com.tangem.grow.datasource.gasless.models.solana.SolanaSubmitRequestBody
import com.tangem.grow.datasource.gasless.models.solana.SolanaSubmitResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface SolanaGaslessApi {

    @GET("api/v1/solana/config/fee-payer")
    suspend fun getFeePayer(): ApiResponse<GaslessServiceResponse<SolanaFeePayerResponse>>

    /** 40s: the backend route completes within 26s, plus margin. */
    @ReadTimeout(duration = 40, unit = TimeUnit.SECONDS)
    @POST("api/v1/solana/transaction/submit")
    suspend fun submit(
        @Header("userWalletId") userWalletId: String,
        @Body body: SolanaSubmitRequestBody,
    ): ApiResponse<GaslessServiceResponse<SolanaSubmitResponse>>
}