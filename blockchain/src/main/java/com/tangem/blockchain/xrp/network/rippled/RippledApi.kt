package com.tangem.blockchain.xrp.network.rippled

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface RippledApi {
    @Headers("Content-Type: application/json")
    @POST("./")
    suspend fun getAccount(@Body rippledBody: RippledBody): RippledAccountResponse

    @Headers("Content-Type: application/json")
    @POST("./")
    suspend fun getServerState(@Body rippledBody: RippledBody = serverStateBody): RippledStateResponse

    @Headers("Content-Type: application/json")
    @POST("./")
    suspend fun getFee(@Body rippledBody: RippledBody = feeBody): RippledFeeResponse

    @Headers("Content-Type: application/json")
    @POST("./")
    suspend fun submitTransaction(@Body rippledBody: RippledBody): RippledSubmitResponse
}

enum class RippledMethod(val value: String) {
    ACCOUNT_INFO("account_info"),
    SERVER_STATE("server_state"),
    FEE("fee"),
    SUBMIT("submit")
}

data class RippledBody(
        val method: String,
        val params: HashMap<String, String> = HashMap() //TODO =null?
)

val serverStateBody = RippledBody(RippledMethod.SERVER_STATE.value)
val feeBody = RippledBody(RippledMethod.FEE.value)