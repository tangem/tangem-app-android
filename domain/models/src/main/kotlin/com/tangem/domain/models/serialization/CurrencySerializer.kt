package com.tangem.domain.models.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Currency

typealias SerializedCurrency = @Serializable(with = CurrencySerializer::class) Currency

object CurrencySerializer : KSerializer<Currency> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.util.Currency", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Currency) {
        encoder.encodeString(value.currencyCode)
    }

    override fun deserialize(decoder: Decoder): Currency {
        return Currency.getInstance(decoder.decodeString())
    }
}