package com.tangem.data.networks.entity

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Stores [NetworkStatusDM] the way the Moshi name-polymorphic adapter did: no discriminator is written, and the
 * subtype is picked by the key only it has.
 */
internal object NetworkStatusDMSerializer : JsonContentPolymorphicSerializer<NetworkStatusDM>(NetworkStatusDM::class) {

    private const val VERIFIED_KEY = "amounts"
    private const val NO_ACCOUNT_KEY = "amount_to_create_account"

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<NetworkStatusDM> {
        val keys = element.jsonObject.keys

        return when {
            VERIFIED_KEY in keys -> NetworkStatusDM.Verified.serializer()
            NO_ACCOUNT_KEY in keys -> NetworkStatusDM.NoAccount.serializer()
            else -> throw SerializationException("Unknown network status with keys $keys")
        }
    }
}