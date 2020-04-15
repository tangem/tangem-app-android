package com.tangem.di

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import com.tangem.data.network.Server
import com.tangem.wallet.BuildConfig
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import javax.inject.Named
import javax.inject.Singleton

@Module
internal class NetworkModule {

    @Singleton
    @Provides
    @Named(Server.ApiInfura.URL_INFURA)
    fun provideRetrofitInfura(): Retrofit {
        val builder = Retrofit.Builder()
                .baseUrl(Server.ApiInfura.URL_INFURA)
                .addConverterFactory(GsonConverterFactory.create())
        if (BuildConfig.DEBUG)
            builder.client(createOkHttpClient())
        return builder.build()
    }

    @Singleton
    @Provides
    @Named(Server.ApiInfuraTestnet.URL_INFURA_TESTNET)
    fun provideRetrofitInfuraTestnet(): Retrofit {
        val builder = Retrofit.Builder()
                .baseUrl(Server.ApiInfuraTestnet.URL_INFURA_TESTNET)
                .addConverterFactory(GsonConverterFactory.create())
        if (BuildConfig.DEBUG)
            builder.client(createOkHttpClient())
        return builder.build()
    }

    @Singleton
    @Provides
    @Named(Server.ApiInfuraRopsten.URL_INFURA_ROPSTEN)
    fun provideRetrofitInfuraRopsten(): Retrofit {
        val builder = Retrofit.Builder()
                .baseUrl(Server.ApiInfuraRopsten.URL_INFURA_ROPSTEN)
                .addConverterFactory(GsonConverterFactory.create())
        if (BuildConfig.DEBUG)
            builder.client(createOkHttpClient())
        return builder.build()
    }

    @Singleton
    @Provides
    @Named(Server.ApiRootstock.URL_ROOTSTOCK)
    fun provideRetrofitRootstock(): Retrofit {
        val builder = Retrofit.Builder()
                .baseUrl(Server.ApiRootstock.URL_ROOTSTOCK)
                .addConverterFactory(GsonConverterFactory.create())
        if (BuildConfig.DEBUG)
            builder.client(createOkHttpClient())
        return builder.build()
    }

    @Singleton
    @Provides
    @Named(Server.ApiMaticTesnet.URL_MATIC_TESTNET)
    fun provideRetrofitMaticTestnet(): Retrofit {
        val builder = Retrofit.Builder()
                .baseUrl(Server.ApiMaticTesnet.URL_MATIC_TESTNET)
                .addConverterFactory(GsonConverterFactory.create())
        if (BuildConfig.DEBUG)
            builder.client(createOkHttpClient())
        return builder.build()
    }

    @Singleton
    @Provides
    @Named(Server.ApiEstimatefee.URL_ESTIMATEFEE)
    fun provideRetrofitEstimatefee(): Retrofit {
        val builder = Retrofit.Builder()
                .baseUrl(Server.ApiEstimatefee.URL_ESTIMATEFEE)
                .addConverterFactory(GsonConverterFactory.create())
        if (BuildConfig.DEBUG)
            builder.client(createOkHttpClient())
        return builder.build()
    }

    @Singleton
    @Provides
    @Named(Server.ApiUpdateVersion.URL_UPDATE_VERSION)
    fun provideGithubusercontent(): Retrofit {
        val builder = Retrofit.Builder()
                .baseUrl(Server.ApiUpdateVersion.URL_UPDATE_VERSION)
                .addConverterFactory(GsonConverterFactory.create())
                .client(createOkHttpClient())
        if (BuildConfig.DEBUG)
            builder.client(createOkHttpClient())
        return builder.build()
    }

    @Singleton
    @Provides
    @Named(Server.ApiCoinmarket.URL_COINMARKET)
    fun provideRetrofitCoinmarketcap(): Retrofit {
        val builder = Retrofit.Builder()
                .baseUrl(Server.ApiCoinmarket.URL_COINMARKET)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
//        if (BuildConfig.DEBUG)
//            builder.client(createOkHttpClient())
        return builder.build()
    }

    @Singleton
    @Provides
    @Named(Server.ApiBlockcypher.URL_BLOCKCYPHER)
    fun provideRetrofitBlockcypher(): Retrofit {
        val builder = Retrofit.Builder()
                .baseUrl(Server.ApiBlockcypher.URL_BLOCKCYPHER)
                .addConverterFactory(GsonConverterFactory.create())
                .client(createOkHttpClient())
        if (BuildConfig.DEBUG)
            builder.client(createOkHttpClient())
        return builder.build()
    }

    @Singleton
    @Provides
    @Named(Server.ApiSoChain.URL)
    fun provideRetrofitSoChain(): Retrofit {
        val builder = Retrofit.Builder()
                .baseUrl(Server.ApiSoChain.URL)
                .addConverterFactory(GsonConverterFactory.create())
        if (BuildConfig.DEBUG)
            builder.client(createOkHttpClient())
        return builder.build()
    }

    @Singleton
    @Provides
    @Named(Server.ApiBlockchainInfo.URL_BLOCKCHAININFO)
    fun provideRetrofitBlockchainInfo(): Retrofit {
        val builder = Retrofit.Builder()
                .baseUrl(Server.ApiBlockchainInfo.URL_BLOCKCHAININFO)
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        if (BuildConfig.DEBUG)
            builder.client(createOkHttpClient())
        return builder.build()
    }

    @Singleton
    @Provides
    @Named(Server.ApiDucatus.URL_DUCATUS)
    fun provideRetrofitDucatus(): Retrofit {
        val builder = Retrofit.Builder()
                .baseUrl(Server.ApiDucatus.URL_DUCATUS)
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        if (BuildConfig.DEBUG)
            builder.client(createOkHttpClient())
        return builder.build()
    }

    @Singleton
    @Provides
    @Named(Server.ApiBlockchair.URL_BLOCKCHAIR)
    fun provideRetrofitBlockchair(): Retrofit {
        val builder = Retrofit.Builder()
                .baseUrl(Server.ApiBlockchair.URL_BLOCKCHAIR)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
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

    @Provides
    @Named("socket")
    fun provideSocket(): Socket {
        val socket = Socket()
        try {
            socket.soTimeout = 2000
            try {
                socket.bind(InetSocketAddress(0))
            } catch (e: IOException) {
                e.printStackTrace()
            }

        } catch (e: SocketException) {
            e.printStackTrace()
        }

        return socket
    }

}