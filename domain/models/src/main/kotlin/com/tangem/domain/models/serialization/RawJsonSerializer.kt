package com.tangem.domain.models.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder

/**
 * A JSON value carried as the text of the JSON itself instead of being decoded into Kotlin types.
 *
 * Meant for a value the app hashes rather than reads: number literals stay exactly as the server sent them
 * (`137` never becomes `137.0`), so a digest taken over the text keeps matching the one the server computed.
 */
typealias SerializedRawJson = @Serializable(with = RawJsonSerializer::class) String

object RawJsonSerializer : KSerializer<String> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("RawJson", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        val jsonEncoder = encoder as? JsonEncoder ?: return encoder.encodeString(value)

        jsonEncoder.encodeJsonElement(Json.parseToJsonElement(value))
    }

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()

        return jsonDecoder.decodeJsonElement().toString()
    }
}