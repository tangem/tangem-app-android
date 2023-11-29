package com.tangem.datasource.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.oneinch.OneInchApi
import com.tangem.datasource.api.oneinch.OneInchApiFactory
import com.tangem.datasource.utils.RequestHeader
import com.tangem.datasource.utils.addHeaders
import com.tangem.datasource.utils.addLoggers
import com.tangem.lib.auth.AuthBearerProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideOneInchApiFactory(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        auth1Inch: AuthBearerProvider,
    ): OneInchApiFactory {
        val networks = mapOf(
            ETH_NETWORK to ONE_INCH_ETH_PATH,
            BSC_NETWORK to ONE_INCH_BSC_PATH,
            POLYGON_NETWORK to ONE_INCH_POLYGON_PATH,
            OPTIMISM_NETWORK to ONE_INCH_OPTIMISM_PATH,
            ARBITRUM_NETWORK to ONE_INCH_ARBITRUM_PATH,
            GNOSIS_NETWORK to ONE_INCH_GNOSIS_PATH,
            AVALANCHE_NETWORK to ONE_INCH_AVALANCHE_PATH,
            FANTOM_NETWORK to ONE_INCH_FANTOM_PATH,
        )

        val apiFactory = OneInchApiFactory()

        for ((network, path) in networks) {
            apiFactory.putApi(
                networkId = network,
                api = createOneInchApiWithUrl("$ONE_INCH_BASE_URL$path", moshi, context, auth1Inch),
            )
        }

        return apiFactory
    }

    private fun createOneInchApiWithUrl(
        url: String,
        moshi: Moshi,
        context: Context,
        auth1Inch: AuthBearerProvider,
    ): OneInchApi {
        return Retrofit.Builder()
            .addConverterFactory(
                MoshiConverterFactory.create(moshi),
            )
            .baseUrl(url)
            .client(
                OkHttpClient.Builder()
                    .addHeaders(RequestHeader.AuthBearerHeader(auth1Inch))
                    .addLoggers(context)
                    .build(),
            )
            .build()
            .create(OneInchApi::class.java)
    }

    companion object {
        private const val ONE_INCH_BASE_URL = "https://api.1inch.dev/swap/v5.2/"
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