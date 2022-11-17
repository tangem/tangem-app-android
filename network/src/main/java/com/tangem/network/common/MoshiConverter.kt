package com.tangem.network.common

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.tangem.common.json.MoshiJsonConverter
import retrofit2.Converter
import retrofit2.converter.moshi.MoshiConverterFactory

/**
[REDACTED_AUTHOR]
 */
object MoshiConverter {

    var INSTANCE = MoshiJsonConverter()
        private set

    fun reInitInstance(
        adapters: List<Any> = listOf(),
        typedAdapters: Map<Class<*>, JsonAdapter<*>> = mapOf(),
    ) {
        INSTANCE = MoshiJsonConverter(adapters, typedAdapters)
    }

    fun createFactory(moshi: Moshi = INSTANCE.moshi): Converter.Factory = MoshiConverterFactory.create(moshi)

    fun defaultMoshi(): Moshi = INSTANCE.moshi

    fun sdkMoshi(): Moshi = MoshiJsonConverter.INSTANCE.moshi
}