package com.tangem.commands.common.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


class ApiTangem {
    companion object {
        const val TANGEM_ENDPOINT: String = "https://verify.tangem.com/"

        const val VERIFY = "verify"
        const val VERIFY_AND_GET_INFO =  "card/verify-and-get-info"
        const val ARTWORK = "card/artwork"
    }

}

private val moshi: Moshi by lazy {
    Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
}

fun createRetrofitInstance(baseUrl: String): Retrofit =
        Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
