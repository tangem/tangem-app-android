package com.tangem.blockchain.blockchains.ethereum.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface EthereumApi {
    @Headers("Content-Type: application/json")
    @POST("{apiKey}")
    suspend fun post(@Body body: EthereumBody?, @Path("apiKey") apiKey: String): EthereumResponse
}

@JsonClass(generateAdapter = true)
data class EthereumBody(
        val jsonrpc: String = "2.0",
        val id: Int = 67,
        val method: String? = null,
        val params: List<Any> = listOf()
)

data class EthCallParams(private val data: String, private val to: String)

enum class EthereumMethod(val value: String) {
    GET_BALANCE("eth_getBalance"),
    GET_TRANSACTION_COUNT("eth_getTransactionCount"),
    GET_PENDING_COUNT("eth_getPendingCount"),
    CALL("eth_call"),
    SEND_RAW_TRANSACTION("eth_sendRawTransaction"),
    GAS_PRICE("eth_gasPrice")
}
