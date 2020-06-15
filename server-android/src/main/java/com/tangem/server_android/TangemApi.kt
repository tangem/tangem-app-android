package com.tangem.server_android

import com.tangem.server_android.model.CardVerifyAndGetInfo
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface TangemApi {
    @Headers("Content-Type: application/json")
    @POST(Server.ApiTangem.Method.VERIFY_AND_GET_INFO)
    fun getCardVerifyAndGetInfo(@Body requestBody: CardVerifyAndGetInfo.Request?): Call<CardVerifyAndGetInfo.Response>

    @GET(Server.ApiTangem.Method.ARTWORK)
    fun getArtwork(
        @Query("artworkId") artworkId: String?,
        @Query("CID") CID: String?,
        @Query("publicKey") publicKey: String?
    ): Call<ResponseBody>

    @GET(Server.ApiTangem.Method.PAY_ID)
    suspend fun getPayId(
        @Query("cid") cardId: String,
        @Query("key") publicKey: String
    ): PayIdResponse

    @POST(Server.ApiTangem.Method.PAY_ID)
    suspend fun setPayId(
        @Query("cid") cardId: String,
        @Query("key") publicKey: String,
        @Query("payid") payId: String,
        @Query("address") address: String,
        @Query("network") network: String
    ): SetPayIdResponse

}