package com.tangem.tap.network.payid

import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.datasource.api.common.createRetrofitInstance

/**
[REDACTED_AUTHOR]
 */
class PayIdVerifyService(
    private val baseUrl: String,
) {

    private val api = createRetrofitInstance(
        baseUrl = baseUrl,
        logEnabled = false,
    ).create(PayIdVerifyApi::class.java)

    suspend fun verifyAddress(user: String, network: String): Result<VerifyPayIdResponse> {
        return performRequest { api.verifyAddress(user, createNetworkHeader(network)) }
    }

    private fun createNetworkHeader(network: String): String = "application/$network-mainnet+json"
}