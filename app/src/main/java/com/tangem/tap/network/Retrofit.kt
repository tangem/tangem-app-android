package com.tangem.tap.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.wallet.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.math.BigDecimal

fun createRetrofitInstance(
        baseUrl: String,
        interceptors: List<Interceptor> = emptyList(),
): Retrofit {
    val okHttpBuilder = OkHttpClient.Builder()
    interceptors.forEach { okHttpBuilder.addInterceptor(it) }

    if (BuildConfig.DEBUG) okHttpBuilder.addInterceptor(createHttpLoggingInterceptor())
    return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(createMoshiConverterFactory())
            .client(okHttpBuilder.build())
            .build()
}

fun createMoshiConverterFactory(): Converter.Factory = MoshiConverterFactory.create(createMoshi())

fun createMoshi(): Moshi = Moshi.Builder()
        .add(BigDecimalAdapter)
        .add(KotlinJsonAdapterFactory()).build()

private fun createHttpLoggingInterceptor(): HttpLoggingInterceptor {
    val logging = HttpLoggingInterceptor()
    logging.level = HttpLoggingInterceptor.Level.BODY
    return logging
}

private object BigDecimalAdapter {
    @FromJson
    fun fromJson(string: String) = BigDecimal(string)

    @ToJson
    fun toJson(value: BigDecimal) = value.toString()
}