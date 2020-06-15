package com.tangem.server_android

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PayIdService {

    private val tangemApi: TangemApi by lazy {
        provideRetrofit()
            .create(TangemApi::class.java)
    }

    suspend fun getPayId(cardId: String, publicKey: String): Result<PayIdResponse> {
        return performRequest { tangemApi.getPayId(cardId, publicKey) }
    }

    suspend fun setPayId(cardId: String, publicKey: String, payId: String, address: String, network: String): Result<SetPayIdResponse> {
        return performRequest { tangemApi.setPayId(cardId, publicKey, payId, address, network) }
    }
}

data class PayIdResponse(
    val payId: String
)

data class SetPayIdResponse(
    val success: Boolean
)

fun provideRetrofit(): Retrofit {
    val builder = Retrofit.Builder()
        .baseUrl("https://tangem.com/")
        .addConverterFactory(GsonConverterFactory.create())
    if (BuildConfig.DEBUG)
        builder.client(createOkHttpClient())
    return builder.build()
}

private fun createOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder().addInterceptor(createHttpLoggingInterceptor()).build()
}

private fun createHttpLoggingInterceptor(): HttpLoggingInterceptor {
    val logging = HttpLoggingInterceptor()
    logging.level = HttpLoggingInterceptor.Level.BODY
    return logging
}