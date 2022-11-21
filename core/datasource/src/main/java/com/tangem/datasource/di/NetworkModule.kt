package com.tangem.datasource.di

import com.tangem.datasource.api.AuthHeaderInterceptor
import com.tangem.datasource.api.referral.ReferralApi
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
    fun provideReferralApi(okHttpClient: OkHttpClient): ReferralApi {
        return Retrofit.Builder()
            .addConverterFactory(
                MoshiConverterFactory.create(),
            )
            .baseUrl(PROD_REFERRAL_BASE_URL)
            .client(okHttpClient)
            .build()
            .create(ReferralApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authProvider: AuthProvider): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC),
            )
            .addInterceptor(AuthHeaderInterceptor(authProvider))
            .build()
    }

    private companion object {
        const val PROD_REFERRAL_BASE_URL = "https://api.tangem-tech.com/v1/"
    }
}