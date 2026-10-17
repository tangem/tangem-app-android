package com.tangem.core.remote.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

object NetworkJsonFactory {

    @OptIn(ExperimentalSerializationApi::class)
    fun create(serializersModule: SerializersModule = EmptySerializersModule()): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
        decodeEnumsCaseInsensitive = true
        this.serializersModule = serializersModule
    }
}