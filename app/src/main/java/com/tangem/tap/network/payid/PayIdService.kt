package com.tangem.tap.network.payid

import com.squareup.moshi.JsonClass
import com.tangem.commands.common.network.Result
import com.tangem.commands.common.network.performRequest
import com.tangem.tap.network.createRetrofitInstance
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

    private fun provideRetrofit(): Retrofit = createRetrofitInstance("https://tangem.com/")

}

@JsonClass(generateAdapter = true)
data class PayIdResponse(
        val payId: String
)

@JsonClass(generateAdapter = true)
data class SetPayIdResponse(
        val success: Boolean
)

