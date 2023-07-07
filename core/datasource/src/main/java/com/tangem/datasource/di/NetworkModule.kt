package com.tangem.datasource.di

import com.squareup.moshi.Moshi
import com.tangem.datasource.api.paymentology.PaymentologyApi
import com.tangem.datasource.api.promotion.PromotionApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.utils.RequestHeader.*
import com.tangem.datasource.utils.addHeaders
import com.tangem.datasource.utils.allowLogging
import com.tangem.lib.auth.AuthProvider
import com.tangem.lib.auth.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    @Provides
    @Singleton
    fun provideTangemTechApi(@NetworkMoshi moshi: Moshi): TangemTechApi {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(PROD_TANGEM_TECH_BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addHeaders(
                        CacheControlHeader,
// [REDACTED_TODO_COMMENT]
                        // AuthenticationHeader(authProvider),
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

    @Provides
    @Singleton
    @PromotionOneInch
    fun providePromotionOneInchApi(authProvider: AuthProvider, @NetworkMoshi moshi: Moshi): PromotionApi {
        val okClient = OkHttpClient.Builder()
            .addHeaders(AuthenticationHeader(authProvider))
            .allowLogging()
            .callTimeout(API_ONE_INCH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .connectTimeout(API_ONE_INCH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(API_ONE_INCH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(API_ONE_INCH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build()
        return createBasePromotionRetrofit(okClient, moshi)
    }

    private fun createBasePromotionRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): PromotionApi {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(if (BuildConfig.DEBUG) DEV_TANGEM_TECH_BASE_URL else PROD_TANGEM_TECH_BASE_URL)
            .client(okHttpClient)
            .build()
            .create(PromotionApi::class.java)
    }

    private companion object {
        const val PROD_TANGEM_TECH_BASE_URL = "https://api.tangem-tech.com/v1/"
        const val DEV_TANGEM_TECH_BASE_URL = "https://devapi.tangem-tech.com/v1/"

        const val PAYMENTOLOGY_BASE_URL: String = "https://paymentologygate.oa.r.appspot.com/"
        const val API_ONE_INCH_TIMEOUT_MS = 5000L
    }
}
