package com.tangem.domain.models.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.joda.time.DateTime

typealias SerializedDateTime = @Serializable(with = JodaDateTimeSerializer::class) DateTime

object JodaDateTimeSerializer : KSerializer<DateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DateTime", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: DateTime) {
        encoder.encodeLong(value.millis)
    }

    override fun deserialize(decoder: Decoder): DateTime {
        return DateTime(decoder.decodeLong())
    }
}