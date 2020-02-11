package com.tangem.blockchain.common.network

import com.tangem.blockchain.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder().apply {
        if (BuildConfig.DEBUG) addInterceptor(createHttpLoggingInterceptor())
    }.build()
}

private fun createHttpLoggingInterceptor(): HttpLoggingInterceptor {
    val logging = HttpLoggingInterceptor()
    logging.level = HttpLoggingInterceptor.Level.BODY
    return logging
}


fun createRetrofitInstance(baseUrl: String): Retrofit =
        Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(MoshiConverterFactory.create())
                .client(okHttpClient)
                .build()


const val API_TANGEM = "https://verify.tangem.com/"
const val API_COINMARKETCAP = "https://pro-api.coinmarketcap.com/"
const val API_INFURA = "https://mainnet.infura.io/"
const val API_SOCHAIN_V2 = "https://chain.so/"
const val API_ESTIMATEFEE = "https://estimatefee.com/"
const val API_UPDATE_VERSION = "https://raw.githubusercontent.com/"
const val API_ROOTSTOCK = "https://public-node.rsk.co/"
const val API_BLOCKCYPHER = "https://api.blockcypher.com/"
const val API_BINANCE = "https://dex.binance.org/"
const val API_BINANCE_TESTNET = "https://testnet-dex.binance.org/"
const val API_MATIC_TESTNET = "https://testnet2.matic.network/"
const val API_STELLAR = "https://horizon.stellar.org/"
const val API_STELLAR_RESERVE = "https://horizon.sui.li/"
const val API_STELLAR_TESTNET = "https://horizon-testnet.stellar.org/"
const val API_BLOCKCHAIN_INFO = "https://blockchain.info/"
const val API_ADALITE = "https://explorer2.adalite.io"
const val API_ADALITE_RESERVE = "https://nodes.southeastasia.cloudapp.azure.com"
const val API_RIPPLED = "https://s1.ripple.com:51234"
const val API_RIPPLED_RESERVE = "https://s2.ripple.com:51234"