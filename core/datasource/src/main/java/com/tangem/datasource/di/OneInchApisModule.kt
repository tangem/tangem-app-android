package com.tangem.datasource.di

import com.squareup.moshi.Moshi
import com.tangem.datasource.api.oneinch.OneInchApi
import com.tangem.datasource.api.oneinch.OneInchApiFactory
import com.tangem.datasource.utils.allowLogging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class OneInchApisModule {

    @Provides
    @Singleton
    fun provideOneInchApiFactory(@NetworkMoshi moshi: Moshi): OneInchApiFactory {
        val apiFactory = OneInchApiFactory()
        apiFactory.putApi(ETH_NETWORK, createOneInchApiWithUrl(ONE_INCH_BASE_URL + ONE_INCH_ETH_PATH, moshi))
        apiFactory.putApi(BSC_NETWORK, createOneInchApiWithUrl(ONE_INCH_BASE_URL + ONE_INCH_BSC_PATH, moshi))
        apiFactory.putApi(POLYGON_NETWORK, createOneInchApiWithUrl(ONE_INCH_BASE_URL + ONE_INCH_POLYGON_PATH, moshi))
        apiFactory.putApi(OPTIMISM_NETWORK, createOneInchApiWithUrl(ONE_INCH_BASE_URL + ONE_INCH_OPTIMISM_PATH, moshi))
        apiFactory.putApi(ARBITRUM_NETWORK, createOneInchApiWithUrl(ONE_INCH_BASE_URL + ONE_INCH_ARBITRUM_PATH, moshi))
        apiFactory.putApi(GNOSIS_NETWORK, createOneInchApiWithUrl(ONE_INCH_BASE_URL + ONE_INCH_GNOSIS_PATH, moshi))
        apiFactory.putApi(
            AVALANCHE_NETWORK,
            createOneInchApiWithUrl(ONE_INCH_BASE_URL + ONE_INCH_AVALANCHE_PATH, moshi),
        )
        apiFactory.putApi(FANTOM_NETWORK, createOneInchApiWithUrl(ONE_INCH_BASE_URL + ONE_INCH_FANTOM_PATH, moshi))
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
                    .allowLogging()
                    .build(),
            )
            .build()
            .create(OneInchApi::class.java)
    }

    companion object {
        private const val ONE_INCH_BASE_URL = "https://api.1inch.io/v5.0/"
        private const val ONE_INCH_ETH_PATH = "1/"
        private const val ONE_INCH_BSC_PATH = "56/"
        private const val ONE_INCH_POLYGON_PATH = "137/"
        private const val ONE_INCH_OPTIMISM_PATH = "10/"
        private const val ONE_INCH_ARBITRUM_PATH = "42161/"
        private const val ONE_INCH_GNOSIS_PATH = "100/"
        private const val ONE_INCH_AVALANCHE_PATH = "43114/"
        private const val ONE_INCH_FANTOM_PATH = "250/"

        private const val ETH_NETWORK = "ethereum"
        private const val BSC_NETWORK = "binance-smart-chain"
        private const val POLYGON_NETWORK = "polygon-pos"
        private const val OPTIMISM_NETWORK = "optimistic-ethereum"
        private const val ARBITRUM_NETWORK = "arbitrum-one"
        private const val GNOSIS_NETWORK = "xdai"
        private const val AVALANCHE_NETWORK = "avalanche"
        private const val FANTOM_NETWORK = "fantom"
    }
}
