package com.tangem.tap.network.moonpay

import com.squareup.moshi.JsonClass
import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.tap.network.createRetrofitInstance

class MoonpayService {

    private val moonpayApi: MoonpayApi by lazy {
        createRetrofitInstance(MoonpayApi.MOOONPAY_BASE_URL)
            .create(MoonpayApi::class.java)
    }

    suspend fun getUserStatus(moonpayApiKey: String): Result<MoonPayUserStatus> {
        return performRequest { moonpayApi.getUserStatus(moonpayApiKey) }
    }
}

@JsonClass(generateAdapter = true)
data class MoonPayUserStatus(
    val isBuyAllowed: Boolean,
    val isSellAllowed: Boolean
)