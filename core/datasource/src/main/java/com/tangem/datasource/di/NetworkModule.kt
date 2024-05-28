package com.tangem.datasource.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.response.ApiResponseCallAdapterFactory
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.TangemTechApiV2
import com.tangem.datasource.api.tangemTech.TangemTechServiceApi
import com.tangem.datasource.utils.RequestHeader.*
import com.tangem.datasource.utils.addHeaders
import com.tangem.datasource.utils.addLoggers
import com.tangem.lib.auth.AppVersionProvider
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
        appVersionProvider: AppVersionProvider,
    ): TangemExpressApi {
        val url = if (BuildConfig.ENVIRONMENT == "dev") {
            DEV_EXPRESS_BASE_URL
        } else {
            PROD_EXPRESS_BASE_URL
        }
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .baseUrl(url)
            .client(
                OkHttpClient.Builder()
                    .addHeaders(Express(expressAuthProvider))
                    .addHeaders(AppVersionPlatformHeaders(appVersionProvider))
                    .addLoggers(context)
                    .build(),
            )
            .build()
            .create(TangemExpressApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTangemTechApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        appVersionProvider: AppVersionProvider,
    ): TangemTechApi {
        return provideTangemTechApiInternal(moshi, context, appVersionProvider, PROD_V1_TANGEM_TECH_BASE_URL)
    }

    @Provides
    @Singleton
    fun provideTangemTechApiV2(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        appVersionProvider: AppVersionProvider,
    ): TangemTechApiV2 {
        return provideTangemTechApiInternal(moshi, context, appVersionProvider, PROD_V2_TANGEM_TECH_BASE_URL)
    }

    @Provides
    @DevTangemApi
    @Singleton
    fun provideTangemTechDevApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        appVersionProvider: AppVersionProvider,
    ): TangemTechApi {
        return provideTangemTechApiInternal(moshi, context, appVersionProvider, DEV_V1_TANGEM_TECH_BASE_URL)
    }

    @Provides
    @Singleton
    fun provideTangemTechServiceApi(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        appVersionProvider: AppVersionProvider,
    ): TangemTechServiceApi {
        return provideTangemTechApiInternal(
            moshi,
            context,
            appVersionProvider,
            PROD_V1_TANGEM_TECH_BASE_URL,
            timeoutSeconds = TANGEM_TECH_SERVICE_TIMEOUT_SECONDS,
        )
    }

    private inline fun <reified T> provideTangemTechApiInternal(
        moshi: Moshi,
        context: Context,
        appVersionProvider: AppVersionProvider,
        baseUrl: String,
        timeoutSeconds: Long? = null,
    ): T {
        val client = OkHttpClient.Builder()
            .let { builder ->
                if (timeoutSeconds != null) {
                    builder.callTimeout(timeoutSeconds, TimeUnit.SECONDS)
                } else {
                    builder
                }
            }
            .addHeaders(
                CacheControlHeader,
                AppVersionPlatformHeaders(appVersionProvider),
// [REDACTED_TODO_COMMENT]
                // AuthenticationHeader(authProvider),
            )
            .addLoggers(context)
            .build()

        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .baseUrl(baseUrl)
            .client(client)
            .build()
            .create(T::class.java)
    }

    private companion object {
        const val PROD_EXPRESS_BASE_URL = "https://express.tangem.com/v1/"
        const val DEV_EXPRESS_BASE_URL = "https://express.tangem.org/v1/"

        const val DEV_V1_TANGEM_TECH_BASE_URL = "https://devapi.tangem-tech.com/v1/"

        const val PROD_V1_TANGEM_TECH_BASE_URL = "https://api.tangem-tech.com/v1/"
        const val PROD_V2_TANGEM_TECH_BASE_URL = "https://api.tangem-tech.com/v2/"

        const val TANGEM_TECH_SERVICE_TIMEOUT_SECONDS = 5L

        const val PAYMENTOLOGY_BASE_URL: String = "https://paymentologygate.oa.r.appspot.com/"
        const val API_ONE_INCH_TIMEOUT_MS = 5000L
    }
}
