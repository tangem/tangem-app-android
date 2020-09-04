package com.tangem.tap.network.payid

import com.tangem.commands.common.network.Result
import com.tangem.commands.common.network.performRequest
import com.tangem.tap.network.createRetrofitInstance

/**
[REDACTED_AUTHOR]
 */
class PayIdVerifyService(
        private val baseUrl: String
) {

    private val api = createRetrofitInstance(baseUrl).create(PayIdVerifyApi::class.java)

    suspend fun verifyAddress(user: String, network: String): Result<VerifyPayIdResponse> {
        return performRequest { api.verifyAddress(user, createNetworkHeader(network)) }
    }

    private fun createNetworkHeader(network: String): String = "application/$network-mainnet+json"
}