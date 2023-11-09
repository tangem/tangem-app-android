package com.tangem.datasource.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.common.response.ApiResponseCallAdapterFactory
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.promotion.PromotionApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.utils.RequestHeader.*
import com.tangem.datasource.utils.addHeaders
import com.tangem.datasource.utils.addLoggers
import com.tangem.lib.auth.AuthProvider
import com.tangem.lib.auth.ExpressAuthProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideExpressApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        expressAuthProvider: ExpressAuthProvider,
    ): TangemExpressApi {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .baseUrl(DEV_EXPRESS_BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addHeaders(Express(expressAuthProvider))
                    .addLoggers(context)
                    .build(),
            )
            .build()
            .create(TangemExpressApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTangemTechApi(@NetworkMoshi moshi: Moshi, @ApplicationContext context: Context): TangemTechApi {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .baseUrl(PROD_TANGEM_TECH_BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addHeaders(
                        CacheControlHeader,
                        // TODO("refactor header init") get auth data after biometric auth to avoid race condition
                        // AuthenticationHeader(authProvider),
                    )
                    .addLoggers(context)
                    .build(),
            )
            .build()
            .create(TangemTechApi::class.java)
    }

    @Provides
    @Singleton
    @PromotionOneInch
    fun providePromotionOneInchApi(
        authProvider: AuthProvider,
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
    ): PromotionApi {
        val okClient = OkHttpClient.Builder()
            .addHeaders(AuthenticationHeader(authProvider))
            .addLoggers(context)
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
            .baseUrl(PROD_TANGEM_TECH_BASE_URL)
            .client(okHttpClient)
            .build()
            .create(PromotionApi::class.java)
    }

    private companion object {
        const val PROD_EXPRESS_BASE_URL = "[REDACTED_ENV_URL]"
        const val DEV_EXPRESS_BASE_URL = "[REDACTED_ENV_URL]"

        const val PROD_TANGEM_TECH_BASE_URL = "https://api.tangem-tech.com/v1/"
        const val DEV_TANGEM_TECH_BASE_URL = "https://devapi.tangem-tech.com/v1/"

        const val PAYMENTOLOGY_BASE_URL: String = "https://paymentologygate.oa.r.appspot.com/"
        const val API_ONE_INCH_TIMEOUT_MS = 5000L
    }
}