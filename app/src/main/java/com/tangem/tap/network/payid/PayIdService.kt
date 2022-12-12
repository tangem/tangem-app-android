package com.tangem.tap.network.payid

import com.squareup.moshi.JsonClass
import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.datasource.api.common.createRetrofitInstance
import retrofit2.Retrofit

class PayIdService {

    private val payIdApi: PayIdApi by lazy {
        provideRetrofit()
                .create(PayIdApi::class.java)
    }

    suspend fun getPayId(cardId: String, publicKey: String): Result<PayIdResponse> {
        return performRequest { payIdApi.getPayId(cardId, publicKey) }
    }

    suspend fun setPayId(
            cardId: String, publicKey: String, payId: String, address: String, network: String
    ): Result<SetPayIdResponse> {
        return performRequest { payIdApi.setPayId(cardId, publicKey, payId, address, network) }
    }

    private fun provideRetrofit(): Retrofit = createRetrofitInstance(
        baseUrl = "https://tangem.com/",
        logEnabled = false,
    )

}

@JsonClass(generateAdapter = true)
data class PayIdResponse(
        val payId: String
)

@JsonClass(generateAdapter = true)
data class SetPayIdResponse(
        val success: Boolean
)

