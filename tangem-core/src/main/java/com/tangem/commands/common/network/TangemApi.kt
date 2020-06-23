package com.tangem.commands.common.network


import com.tangem.commands.common.network.ApiTangem.Companion.VERIFY_AND_GET_INFO
import com.tangem.commands.verifycard.CardVerifyAndGetInfo
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface TangemApi {

    @Headers("Content-Type: application/json")
    @POST(VERIFY_AND_GET_INFO)
    suspend fun getCardVerifyAndGetInfo(@Body requestBody: CardVerifyAndGetInfo.Request): CardVerifyAndGetInfo.Response


}