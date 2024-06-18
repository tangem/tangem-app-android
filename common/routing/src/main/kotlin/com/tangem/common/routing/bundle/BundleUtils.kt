package com.tangem.common.routing.bundle

import android.os.Bundle
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

val defaultSerializersModule: SerializersModule = EmptySerializersModule()

/**
 * Deserialize this bundle into an object of type [T].
 *
 * @receiver [Bundle] to deserialize.
 * @param deserializer [DeserializationStrategy] of the [T] class.
 *
 * @return Object of type T deserialized from bundle.
 */
@OptIn(ExperimentalSerializationApi::class)
fun <T> Bundle.unbundle(
    deserializer: DeserializationStrategy<T>,
    serializersModule: SerializersModule = defaultSerializersModule,
): T {
    val decoder = BundleDecoder(
        bundle = this,
        elementsCount = -1,
        isInitializer = true,
        serializersModule = serializersModule,
    )

    return deserializer.deserialize(decoder)
}

/**
 * Serialize [T] into a bundle.
 *
 * @receiver Object to serialize.
 * @param serializer [SerializationStrategy] of the [T] class.
 *
 * @return bundle serialized from value
 */
@OptIn(ExperimentalSerializationApi::class)
fun <T> T.bundle(
    serializer: SerializationStrategy<T>,
    serializersModule: SerializersModule = defaultSerializersModule,
): Bundle {
    val bundle = Bundle(serializer.descriptor.elementsCount)
    val encoder = BundleEncoder(
        bundle = bundle,
        parentBundle = null,
        keyInParent = null,
        isInitializer = true,
        serializersModule = serializersModule,
    )

    serializer.serialize(encoder, value = this)

    return bundle
}
