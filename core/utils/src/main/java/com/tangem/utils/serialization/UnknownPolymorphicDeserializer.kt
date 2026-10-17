package com.tangem.utils.serialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.SerializersModuleCollector
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

private const val SEALED_SUBCLASSES_INDEX = 1

inline fun <reified Base : Any> SerializersModuleBuilder.polymorphicUnknownFallback(
    noinline create: (type: String?, payload: JsonObject) -> Base,
) {
    val base = polymorphicBaseSerializer(Base::class, runCatching { serializer<Base>() }.getOrNull())
    polymorphicDefaultDeserializer(Base::class) { type ->
        UnknownPolymorphicDeserializer(baseClass = Base::class, base = base, type = type, create = create)
    }
}

@PublishedApi
@OptIn(ExperimentalSerializationApi::class)
internal fun <Base : Any> polymorphicBaseSerializer(
    baseClass: KClass<Base>,
    serializer: KSerializer<Base>?,
): KSerializer<Base> = serializer?.takeIf { it.descriptor.kind == PolymorphicKind.SEALED }
    ?: PolymorphicSerializer(baseClass)

class UnknownPolymorphicDeserializer<Base : Any>(
    private val baseClass: KClass<Base>,
    private val base: KSerializer<Base>,
    private val type: String?,
    private val create: (type: String?, payload: JsonObject) -> Base,
) : DeserializationStrategy<Base> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("UnknownPolymorphic")

    override fun deserialize(decoder: Decoder): Base {
        val jsonDecoder = requireNotNull(decoder as? JsonDecoder) { "Unknown polymorphic fallback is JSON only" }
        val payload = jsonDecoder.decodeJsonElement().jsonObject

        return decodeKnownTypeIgnoringCase(jsonDecoder.json, payload) ?: create(type, payload)
    }

    private fun decodeKnownTypeIgnoringCase(json: Json, payload: JsonObject): Base? {
        val knownType = knownTypes(json).firstOrNull { it.equals(type, ignoreCase = true) }

        return knownType?.let { name ->
            val typed = JsonObject(payload + (base.descriptor.classDiscriminator(json) to JsonPrimitive(name)))
            runCatching { json.decodeFromJsonElement(base, typed) }.getOrNull()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun knownTypes(json: Json): Iterable<String> = if (base.descriptor.kind == PolymorphicKind.SEALED) {
        base.descriptor.getElementDescriptor(SEALED_SUBCLASSES_INDEX).elementNames
    } else {
        SubclassNamesCollector(baseClass).also(json.serializersModule::dumpTo).names
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.classDiscriminator(json: Json): String =
    annotations.filterIsInstance<JsonClassDiscriminator>().firstOrNull()?.discriminator
        ?: json.configuration.classDiscriminator

@OptIn(ExperimentalSerializationApi::class)
private class SubclassNamesCollector(private val baseClass: KClass<*>) : SerializersModuleCollector {

    val names = mutableListOf<String>()

    override fun <T : Any> contextual(
        kClass: KClass<T>,
        provider: (typeArgumentsSerializers: List<KSerializer<*>>) -> KSerializer<*>,
    ) = Unit

    override fun <Base : Any, Sub : Base> polymorphic(
        baseClass: KClass<Base>,
        actualClass: KClass<Sub>,
        actualSerializer: KSerializer<Sub>,
    ) {
        if (baseClass == this.baseClass) names += actualSerializer.descriptor.serialName
    }

    override fun <Base : Any> polymorphicDefaultSerializer(
        baseClass: KClass<Base>,
        defaultSerializerProvider: (value: Base) -> SerializationStrategy<Base>?,
    ) = Unit

    override fun <Base : Any> polymorphicDefaultDeserializer(
        baseClass: KClass<Base>,
        defaultDeserializerProvider: (className: String?) -> DeserializationStrategy<Base>?,
    ) = Unit
}