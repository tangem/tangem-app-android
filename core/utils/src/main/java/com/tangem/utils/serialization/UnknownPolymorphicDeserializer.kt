package com.tangem.utils.serialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.modules.SerializersModuleBuilder

inline fun <reified Base : Any> SerializersModuleBuilder.polymorphicUnknownFallback(
    noinline create: (type: String?, payload: JsonObject) -> Base,
) {
    polymorphicDefaultDeserializer(Base::class) { type -> UnknownPolymorphicDeserializer(type, create) }
}

class UnknownPolymorphicDeserializer<Base : Any>(
    private val type: String?,
    private val create: (type: String?, payload: JsonObject) -> Base,
) : DeserializationStrategy<Base> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("UnknownPolymorphic")

    override fun deserialize(decoder: Decoder): Base {
        val jsonDecoder = requireNotNull(decoder as? JsonDecoder) { "Unknown polymorphic fallback is JSON only" }

        return create(type, jsonDecoder.decodeJsonElement().jsonObject)
    }
}