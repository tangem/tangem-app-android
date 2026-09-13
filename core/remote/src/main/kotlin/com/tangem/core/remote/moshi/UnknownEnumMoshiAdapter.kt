package com.tangem.core.remote.moshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.adapters.EnumJsonAdapter

/**
 * Creates a Moshi adapter for enum types with support for unknown enum values.
 *
 * Lives in core:remote so any module (core or stream) can build its own enum-fallback adapters and
 * contribute them via [NetworkMoshiConfigurer].
 */
object UnknownEnumMoshiAdapter {

    @Suppress("UNCHECKED_CAST")
    fun <T : Enum<T>> create(enumType: Class<out Enum<*>>, defaultValue: Enum<*>): JsonAdapter<out Enum<*>> {
        return EnumJsonAdapter.create(enumType as Class<T>).withUnknownFallback(defaultValue as T)
    }
}