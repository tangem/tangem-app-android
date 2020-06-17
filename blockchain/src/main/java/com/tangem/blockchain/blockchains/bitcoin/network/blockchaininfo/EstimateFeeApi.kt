package com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo

import retrofit2.http.GET

interface EstimatefeeApi {
    @GET(ESTIMATE_FEE_URL + "n/2")
    suspend fun getEstimateFeePriority(): String

    @GET(ESTIMATE_FEE_URL + "n/3")
    suspend fun getEstimateFeeNormal(): String

    @GET(ESTIMATE_FEE_URL + "n/6")
    suspend fun getEstimateFeeMinimal(): String
}

const val ESTIMATE_FEE_URL = "https://estimatefee.com/"