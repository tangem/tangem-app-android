package com.tangem.datasource.di

import com.squareup.moshi.Moshi
import com.tangem.datasource.api.oneinch.OneInchApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class OneInchApisModule {

    @Provides
    @Singleton
    fun provideOneInchApiFactory(moshi: Moshi): OneInchApiFactory {
        val apiFactory = OneInchApiFactory()
        apiFactory.putApi(ETH_NETWORK, createOneInchApiWithUrl(ONE_INCH_BASE_URL + ONE_INCH_ETH_PATH, moshi))
        apiFactory.putApi(BSC_NETWORK, createOneInchApiWithUrl(ONE_INCH_BASE_URL + ONE_INCH_BSC_PATH, moshi))
        return apiFactory
    }

    private fun createOneInchApiWithUrl(url: String, moshi: Moshi): OneInchApi {
        return Retrofit.Builder()
            .addConverterFactory(
                MoshiConverterFactory.create(moshi),
            )
            .baseUrl(url)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(
                        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY),
                    )
                    .build(),
            )
            .build()
            .create(OneInchApi::class.java)
    }

    companion object {
        private const val ONE_INCH_BASE_URL = "https://api.1inch.io/v5.0/"
        private const val ONE_INCH_ETH_PATH = "1/"
        private const val ONE_INCH_BSC_PATH = "56/"

        private const val ETH_NETWORK = "ethereum"
        private const val BSC_NETWORK = "binance-smart-chain"
    }
}
