package com.tangem.blockchainsdk.providers.dev

import com.tangem.blockchainsdk.providers.models.ProviderModel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Stores [ProviderModel] the way Moshi's polymorphic adapter did: the `"type"` label goes first, an unknown label
 * reads as [ProviderModel.UnsupportedType], which itself can't be written.
 */
internal object ProviderModelSerializer : KSerializer<ProviderModel> {

    private const val TYPE_KEY = "type"
    private const val PUBLIC_TYPE = "public"
    private const val PRIVATE_TYPE = "private"

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ProviderModel")

    override fun deserialize(decoder: Decoder): ProviderModel {
        val json = (decoder as JsonDecoder).decodeJsonElement().jsonObject
        val type = json[TYPE_KEY]?.jsonPrimitive?.contentOrNull
            ?: throw SerializationException("Missing \"$TYPE_KEY\" label")

        return when (type) {
            PUBLIC_TYPE -> ProviderModel.Public(url = json.requireString("url"))
            PRIVATE_TYPE -> ProviderModel.Private(name = json.requireString("name"))
            else -> ProviderModel.UnsupportedType
        }
    }

    override fun serialize(encoder: Encoder, value: ProviderModel) {
        val json = when (value) {
            is ProviderModel.Public -> buildJsonObject {
                put(TYPE_KEY, PUBLIC_TYPE)
                put("url", value.url)
            }
            is ProviderModel.Private -> buildJsonObject {
                put(TYPE_KEY, PRIVATE_TYPE)
                put("name", value.name)
            }
            ProviderModel.UnsupportedType -> throw SerializationException("Unsupported provider can't be stored")
        }

        (encoder as JsonEncoder).encodeJsonElement(json)
    }

    private fun JsonObject.requireString(key: String): String {
        return this[key]?.jsonPrimitive?.contentOrNull ?: throw SerializationException("Missing \"$key\"")
    }
}