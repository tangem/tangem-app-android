package com.tangem.datasource.di

import com.squareup.moshi.Moshi
import com.tangem.datasource.api.paymentology.PaymentologyApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.utils.RequestHeader.*
import com.tangem.datasource.utils.addHeaders
import com.tangem.datasource.utils.allowLogging
import com.tangem.lib.auth.AuthProvider
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
class NetworkModule {

    @Provides
    @Singleton
    fun provideTangemTechApi(authProvider: AuthProvider, @NetworkMoshi moshi: Moshi): TangemTechApi {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(PROD_TANGEM_TECH_BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addHeaders(
                        CacheControlHeader,
                        AuthenticationHeader(authProvider),
                    )
                    .allowLogging()
                    .build(),
            )
            .build()
            .create(TangemTechApi::class.java)
    }

    @Provides
    @Singleton
    fun providePaymentologyApi(@NetworkMoshi moshi: Moshi): PaymentologyApi {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(PAYMENTOLOGY_BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .allowLogging()
                    .build(),
            )
            .build()
            .create(PaymentologyApi::class.java)
    }

    private companion object {
        const val PROD_TANGEM_TECH_BASE_URL = "https://api.tangem-tech.com/v1/"
        const val DEV_TANGEM_TECH_BASE_URL = "https://devapi.tangem-tech.com/v1/"

        private const val PAYMENTOLOGY_BASE_URL: String = "https://paymentologygate.oa.r.appspot.com/"
    }
}
