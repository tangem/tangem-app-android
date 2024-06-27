package com.tangem.datasource.api.common.adapter

import com.squareup.moshi.*
import com.squareup.moshi.adapters.EnumJsonAdapter

/**
 * Object to create a adapter for enum types with support for unknown enum values.
 */
object UnknownEnumMoshiAdapter {

    fun <T : Enum<T>> create(enumType: Class<T>, defaultValue: T): JsonAdapter<T> =
        EnumJsonAdapter.create(enumType).withUnknownFallback(defaultValue)
}