package com.tangem.tap.network.payid

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PayIdApi {
    @GET(API_PAY_ID_TANGEM)
    suspend fun getPayId(
            @Query("cid") cardId: String,
            @Query("key") publicKey: String
    ): PayIdResponse

    @POST(API_PAY_ID_TANGEM)
    suspend fun setPayId(
            @Query("cid") cardId: String,
            @Query("key") publicKey: String,
            @Query("payid") payId: String,
            @Query("address") address: String,
            @Query("network") network: String
    ): SetPayIdResponse

    companion object {
        const val API_PAY_ID_TANGEM = "https://payid.tangem.com/"
    }
}

