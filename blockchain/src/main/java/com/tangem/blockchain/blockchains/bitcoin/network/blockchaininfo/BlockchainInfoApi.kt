package com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo

import okhttp3.ResponseBody
import retrofit2.http.*

interface BlockchainInfoApi {
    @GET("rawaddr/{address}?limit=5")
    suspend fun getAddress(@Path("address") address: String): BlockchainInfoAddress

    @GET("unspent")
    suspend fun getUnspents(@Query("active") address: String): BlockchainInfoUnspents

    @FormUrlEncoded
    @POST("pushtx")
    suspend fun sendTransaction(@Field("tx") transaction: String): ResponseBody
}
