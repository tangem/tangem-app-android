package com.tangem.lib.visa.api

import android.util.Log
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.common.response.ApiResponseCallAdapterFactory
import com.tangem.lib.visa.utils.Constants
import com.tangem.lib.visa.utils.Constants.NETWORK_LOGS_TAG
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class VisaApiBuilder(
    private val useDevApi: Boolean,
    private val isNetworkLoggingEnabled: Boolean,
    private val moshi: Moshi,
    private val headers: Map<String, String>,
    private val networkTimeoutSeconds: Long = Constants.NETWORK_TIMEOUT_SECONDS,
) {

    fun build(): VisaApi {
        val okHttpClient = createOkHttpClient()
        val retrofit = createRetrofit(okHttpClient)
        return retrofit.create(VisaApi::class.java)
    }

    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder().apply {
            connectTimeout(networkTimeoutSeconds, TimeUnit.SECONDS)
            readTimeout(networkTimeoutSeconds, TimeUnit.SECONDS)
            writeTimeout(networkTimeoutSeconds, TimeUnit.SECONDS)

            if (isNetworkLoggingEnabled) {
                addInterceptor(createNetworkLoggingInterceptor())
            }

            if (headers.isNotEmpty()) {
                addInterceptor { chain ->
                    val request = chain.request().newBuilder().apply {
                        headers.forEach { (key, value) -> addHeader(key, value) }
                    }.build()

                    chain.proceed(request)
                }
            }
        }

        return builder.build()
    }

    private fun createRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val baseUrl = if (useDevApi) Constants.VISA_API_DEV_URL else Constants.VISA_API_PROD_URL

        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .build()
    }
}

private fun createNetworkLoggingInterceptor(): Interceptor {
    return LoggingInterceptor.Builder()
        .setLevel(Level.BODY)
        .log(Log.VERBOSE)
        .tag(NETWORK_LOGS_TAG)
        .build()
}