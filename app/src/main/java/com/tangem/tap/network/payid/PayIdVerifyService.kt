package com.tangem.tap.network.payid

import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.network.common.createRetrofitInstance

/**
 * Created by Anton Zhilenkov on 03/09/2020.
 */
class PayIdVerifyService(
    private val baseUrl: String,
) {

    private val api = createRetrofitInstance(baseUrl).create(PayIdVerifyApi::class.java)

    suspend fun verifyAddress(user: String, network: String): Result<VerifyPayIdResponse> {
        return performRequest { api.verifyAddress(user, createNetworkHeader(network)) }
    }

    private fun createNetworkHeader(network: String): String = "application/$network-mainnet+json"
}
