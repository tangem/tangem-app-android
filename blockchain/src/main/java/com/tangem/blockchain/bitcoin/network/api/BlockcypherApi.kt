package com.tangem.blockchain.bitcoin.network.api

import com.squareup.moshi.JsonClass
import com.tangem.blockchain.bitcoin.network.response.BlockcypherFee
import com.tangem.blockchain.bitcoin.network.response.BlockcypherResponse
import com.tangem.blockchain.bitcoin.network.response.BlockcypherTx
import retrofit2.http.*

interface BlockcypherApi {
    @GET("v1/{blockchain}/{network}")
    suspend fun getFee(
            @Path("blockchain") blockchain: String,
            @Path("network") network: String
    ): BlockcypherFee

    @GET("v1/{blockchain}/{network}/addrs/{address}?unspentOnly=true&includeScript=true")
    suspend fun getAddressData(
            @Path("blockchain") blockchain: String,
            @Path("network") network: String,
            @Path("address") address: String
    ): BlockcypherResponse

    @GET("v1/{blockchain}/{network}/txs/{txHash}?includeHex=true")
    suspend fun getTransactions(
            @Path("blockchain") blockchain: String,
            @Path("network") network: String,
            @Path("txHash") txHash: String
    ): BlockcypherTx

    @Headers("Content-Type: application/json")
    @POST("v1/{blockchain}/{network}/txs/push")
    suspend fun sendTransaction(
            @Path("blockchain") blockchain: String,
            @Path("network") network: String,
            @Body blockcypherBody: BlockcypherBody,
            @Query("token") token: String
    ): BlockcypherTx
}

@JsonClass(generateAdapter = true)
data class BlockcypherBody(val tx: String)