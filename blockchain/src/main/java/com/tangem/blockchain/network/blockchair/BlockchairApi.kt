package com.tangem.blockchain.network.blockchair

import com.squareup.moshi.JsonClass
import retrofit2.http.*

interface BlockchairApi {
    @GET("{blockchain}/dashboards/address/{address}")
    suspend fun getAddressData(
            @Path("address") address: String,
            @Path("blockchain") blockchain: String,
            @Query("key") key: String
    ): BlockchairAddress

    @GET("{blockchain}/dashboards/transaction/{transaction}")
    suspend fun getTransaction(
            @Path("transaction") transactionHash: String,
            @Path("blockchain") blockchain: String,
            @Query("key") key: String
    ): BlockchairTransaction

    @GET("{blockchain}/stats")
    suspend fun getBlockchainStats(
            @Path("blockchain") blockchain: String,
            @Query("key") key: String
    ): BlockchairStats

    @POST("{blockchain}/push/transaction")
    suspend fun sendTransaction(
            @Body sendBody: BlockchairBody,
            @Path("blockchain") blockchain: String,
            @Query("key") key: String
    )
}

@JsonClass(generateAdapter = true)
data class BlockchairBody(val data: String)