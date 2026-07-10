package com.tangem.domain.addressbook.model.serialization

import arrow.core.getOrElse
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/*
 * The encrypted address-book payload is a cross-platform (iOS) contract. It carries wallet id, contact
 * name and network id as bare strings, so the wrapper domain types must serialize to their underlying
 * string rather than the default `{"field": …}` object. These serializers are applied per-property via
 * `@Serializable(with = …)`, leaving the global serialization of the shared types untouched.
 */

/** Serializes [UserWalletId] as its bare [UserWalletId.stringValue]. */
internal object UserWalletIdAsStringSerializer : KSerializer<UserWalletId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UserWalletId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UserWalletId) = encoder.encodeString(value.stringValue)

    override fun deserialize(decoder: Decoder): UserWalletId = UserWalletId(decoder.decodeString())
}

/** Serializes [Network.RawID] as its bare [Network.RawID.value]. */
internal object NetworkRawIdAsStringSerializer : KSerializer<Network.RawID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Network.RawID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Network.RawID) = encoder.encodeString(value.value)

    override fun deserialize(decoder: Decoder): Network.RawID = Network.RawID(decoder.decodeString())
}

/**
 * Serializes [ContactName] as its bare [ContactName.value]. On read the string goes back through the
 * validating [ContactName.invoke] gateway; an invalid name surfaces as a [SerializationException] (the
 * cipher maps it to `MalformedBlob`).
 */
internal object ContactNameAsStringSerializer : KSerializer<ContactName> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ContactName", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ContactName) = encoder.encodeString(value.value)

    override fun deserialize(decoder: Decoder): ContactName {
        val raw = decoder.decodeString()
        return ContactName(raw).getOrElse { error ->
            throw SerializationException("Invalid contact name in address-book payload: $error")
        }
    }
}