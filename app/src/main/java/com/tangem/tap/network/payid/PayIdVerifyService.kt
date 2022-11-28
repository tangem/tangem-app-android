package com.tangem.tap.network.payid

import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.datasource.api.common.createRetrofitInstance

/**
 * Created by Anton Zhilenkov on 03/09/2020.
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
