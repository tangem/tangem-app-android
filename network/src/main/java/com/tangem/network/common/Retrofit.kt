package com.tangem.network.common

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

// TODO: refactoring: make it better through factory
fun createRetrofitInstance(
    baseUrl: String,
    okHttpBuilder: OkHttpClient.Builder = OkHttpClient.Builder(),
    interceptors: List<Interceptor> = emptyList(),
    converterFactory: Converter.Factory = MoshiConverter.createFactory(),
    logEnabled: Boolean = false
): Retrofit {
    interceptors.forEach { okHttpBuilder.addInterceptor(it) }
    addTimeOuts(okHttpBuilder)

    if (logEnabled) okHttpBuilder.addInterceptor(createHttpLoggingInterceptor())

    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(converterFactory)
        .client(okHttpBuilder.build())
        .build()
}

private fun addTimeOuts(okHttpBuilder: OkHttpClient.Builder) {
    okHttpBuilder.callTimeout(1, TimeUnit.SECONDS)
    okHttpBuilder.connectTimeout(20, TimeUnit.SECONDS)
    okHttpBuilder.readTimeout(20, TimeUnit.SECONDS)
    okHttpBuilder.writeTimeout(20, TimeUnit.SECONDS)
}

private fun createHttpLoggingInterceptor(): HttpLoggingInterceptor {
    return HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
}