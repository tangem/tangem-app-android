package com.tangem.datasource.api.common

import android.util.Log
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

@Suppress("MagicNumber")
@Deprecated("Create and provide by DI")
fun createRetrofitInstance(
    baseUrl: String,
    okHttpBuilder: OkHttpClient.Builder = OkHttpClient.Builder(),
    interceptors: List<Interceptor> = emptyList(),
    logEnabled: Boolean,
): Retrofit {
    okHttpBuilder.apply {
        callTimeout(10, TimeUnit.SECONDS)
        connectTimeout(20, TimeUnit.SECONDS)
        readTimeout(20, TimeUnit.SECONDS)
        writeTimeout(20, TimeUnit.SECONDS)
    }
    interceptors.forEach { okHttpBuilder.addInterceptor(it) }

    if (logEnabled) okHttpBuilder.addInterceptor(createNetworkLoggingInterceptor())

    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(MoshiConverter.networkMoshiConverter)
        .client(okHttpBuilder.build())
        .build()
}

fun createNetworkLoggingInterceptor(): Interceptor {
    return LoggingInterceptor.Builder()
        .setLevel(Level.BODY)
        .log(Log.VERBOSE)
        .build()
}