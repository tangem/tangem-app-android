package com.tangem.datasource.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.datasource.api.AuthHeaderInterceptor
import com.tangem.datasource.api.oneinch.OneInchApi
import com.tangem.datasource.api.referral.ReferralApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.di.qualifiers.OneInchEthereum
import com.tangem.lib.auth.AuthProvider
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
class NetworkModule {

    @Provides
    @Singleton
    fun provideTangemTechApi(okHttpClient: OkHttpClient): TangemTechApi {
        return Retrofit.Builder()
            .addConverterFactory(
                MoshiConverterFactory.create(),
            )
            .baseUrl(DEV_TANGEM_TECH_BASE_URL)
            .client(okHttpClient)
            .build()
            .create(TangemTechApi::class.java)
    }

    @Provides
    @Singleton
    @OneInchEthereum
    fun provideOneInchEthereumApi(): OneInchApi {
        return Retrofit.Builder()
            .addConverterFactory(
                MoshiConverterFactory.create(),
            )
            .baseUrl(ONE_INCH_ETHER_BASE_URL)
            .client(OkHttpClient())
            .build()
            .create(OneInchApi::class.java)
    }

    @Provides
    @Singleton
    fun provideReferralApi(okHttpClient: OkHttpClient): ReferralApi {
        return Retrofit.Builder()
            .addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder()
                        .addLast(KotlinJsonAdapterFactory())
                        .build(),
                ),
            )
            .baseUrl(DEV_TANGEM_TECH_BASE_URL)
            .client(okHttpClient)
            .build()
            .create(ReferralApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authProvider: AuthProvider): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor(authProvider))
            .addInterceptor(
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY),
            )
            .build()
    }

    private companion object {
        const val PROD_TANGEM_TECH_BASE_URL = "https://api.tangem-tech.com/v1/"
        const val DEV_TANGEM_TECH_BASE_URL = "https://devapi.tangem-tech.com/v1/"
        const val ONE_INCH_ETHER_BASE_URL = ""
    }
}