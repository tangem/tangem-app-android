package com.tangem.data.transaction.api

import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.transaction.api.models.SendIntentResponse
import com.tangem.lib.auth.api.RequiresSessionAuth
import retrofit2.http.GET
import retrofit2.http.Path

/** Intents of the Transaction Service, read on the mobile session of the wallets they were raised for. */
internal interface TransactionIntentApi {

    @GET("api/transactions/v1/intents/{intentId}")
    @RequiresSessionAuth
    suspend fun getSendIntent(@Path("intentId") intentId: String): ApiResponse<SendIntentResponse>
}