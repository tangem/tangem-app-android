package com.tangem.datasource.api.common

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.tangem.common.json.MoshiJsonConverter
import retrofit2.Converter
import retrofit2.converter.moshi.MoshiConverterFactory

/**
* [REDACTED_AUTHOR]
 */
// [REDACTED_TODO_COMMENT]
object MoshiConverter {
// [REDACTED_TODO_COMMENT]
    var INSTANCE = MoshiJsonConverter()
        private set

    fun reInitInstance(
        adapters: List<Any> = listOf(),
        typedAdapters: Map<Class<*>, JsonAdapter<*>> = mapOf(),
    ) {
        INSTANCE = MoshiJsonConverter(adapters, typedAdapters)
    }

    fun createFactory(moshi: Moshi = INSTANCE.moshi): Converter.Factory = MoshiConverterFactory.create(moshi)
// [REDACTED_TODO_COMMENT]
    fun defaultMoshi(): Moshi = INSTANCE.moshi
// [REDACTED_TODO_COMMENT]
    fun sdkMoshi(): Moshi = MoshiJsonConverter.INSTANCE.moshi
}
