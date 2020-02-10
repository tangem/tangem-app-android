package com.tangem.blockchain.ethereum.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface InfuraApi {
    @Headers("Content-Type: application/json")
    @POST("v3/613a0b14833145968b1f656240c7d245")
    suspend fun postToInfura(@Body body: InfuraBody?): InfuraResponse
}

@JsonClass(generateAdapter = true)
data class InfuraBody(
        val jsonrpc: String = "2.0",
        val id: Int = 67,
        val method: String? = null,
        val params: List<Any> = listOf()
)

data class EthCallParams(private val data: String, private val to: String)

enum class InfuraMethod(val value: String) {
    GET_BALANCE("eth_getBalance"),
    GET_TRANSACTION_COUNT("eth_getTransactionCount"),
    GET_PENDING_COUNT("eth_getPendingCount"),
    CALL("eth_call"),
    SEND_RAW_TRANSACTION("eth_sendRawTransaction"),
    GAS_PRICE("eth_gasPrice")
}
