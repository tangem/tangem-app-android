package com.tangem.datasource.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.datasource.api.AuthHeaderInterceptor
import com.tangem.datasource.api.common.BigDecimalAdapter
import com.tangem.datasource.api.referral.ReferralApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
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
    fun provideTangemTechApi(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
    ): TangemTechApi {
        return Retrofit.Builder()
            .addConverterFactory(
                MoshiConverterFactory.create(moshi),
            )
            .baseUrl(DEV_TANGEM_TECH_BASE_URL)
            .client(okHttpClient)
            .build()
            .create(TangemTechApi::class.java)
    }

    @Provides
    @Singleton
    fun provideReferralApi(okHttpClient: OkHttpClient, moshi: Moshi): ReferralApi {
        return Retrofit.Builder()
            .addConverterFactory(
                MoshiConverterFactory.create(moshi),
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

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(BigDecimalAdapter())
            .build()
    }

    private companion object {
        const val PROD_TANGEM_TECH_BASE_URL = "https://api.tangem-tech.com/v1/"
        const val DEV_TANGEM_TECH_BASE_URL = "https://devapi.tangem-tech.com/v1/"
    }
}
