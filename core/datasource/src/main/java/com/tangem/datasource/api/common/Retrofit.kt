package com.tangem.datasource.api.common

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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

    if (logEnabled) okHttpBuilder.addInterceptor(createHttpLoggingInterceptor())

    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(MoshiConverter.networkMoshiConverter)
        .client(okHttpBuilder.build())
        .build()
}

private fun createHttpLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}
