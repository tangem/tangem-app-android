package com.tangem.blockchain.cardano.network.api

import com.tangem.blockchain.cardano.network.adalite.AdaliteAddress
import com.tangem.blockchain.cardano.network.adalite.AdaliteSendBody
import com.tangem.blockchain.cardano.network.adalite.AdaliteUnspents
import retrofit2.http.*
import shadow.okhttp3.ResponseBody

interface AdaliteApi {
    @GET("/api/addresses/summary/{address}")
    suspend fun getAddress(@Path("address") address: String): AdaliteAddress

    @Headers("Content-Type: application/json")
    @POST("/api/bulk/addresses/utxo")
    suspend fun getUnspents(@Body address: String): AdaliteUnspents

    @Headers("Content-Type: application/json")
    @POST("/api/v2/txs/signed")
    suspend fun sendTransaction(@Body adaliteBody: AdaliteSendBody): ResponseBody // List<Any>?
}