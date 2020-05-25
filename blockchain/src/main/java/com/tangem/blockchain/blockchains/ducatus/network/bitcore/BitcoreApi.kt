package com.tangem.blockchain.blockchains.ducatus.network.bitcore

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BitcoreApi {
    @GET("api/DUC/mainnet/address/{address}/balance")
    suspend fun getBalance(@Path("address") address: String): BitcoreBalance

    @GET("api/DUC/mainnet/address/{address}/?unspent=true")
    suspend fun getUnspents(@Path("address") address: String): List<BitcoreUtxo>

    @POST("api/DUC/mainnet/tx/send")
    suspend fun sendTransaction(@Body body: BitcoreSendBody): BitcoreSendResponse
}

@JsonClass(generateAdapter = true)
data class BitcoreSendBody(val rawTx: List<String>)