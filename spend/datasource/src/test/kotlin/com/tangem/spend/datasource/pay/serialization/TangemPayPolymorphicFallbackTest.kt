package com.tangem.spend.datasource.pay.serialization

import com.google.common.truth.Truth.assertThat
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.TangemPayAuthApi
import com.tangem.spend.datasource.pay.TangemPayProvisioningApi
import com.tangem.spend.datasource.visa.VisaApi
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.capturedKClass
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.modules.SerializersModuleCollector
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass

internal class TangemPayPolymorphicFallbackTest {

    private val apis = listOf(
        TangemPayApi::class,
        TangemPayProvisioningApi::class,
        TangemPayAuthApi::class,
        VisaApi::class,
    )

    @Test
    fun `GIVEN every Pay API response WHEN polymorphic types are collected THEN each has the unknown fallback`() {
        // Arrange
        val responseTypes = apis.flatMap { api -> api.java.declaredMethods.mapNotNull(::responseBodyType) }
        val module = TangemPaySerializersModuleProvider.provide()

        // Act
        val polymorphicTypes = responseTypes.flatMapTo(mutableSetOf()) { serializer(it).descriptor.polymorphicTypeNames() }
        val typesWithFallback = DefaultDeserializerCollector().also(module::dumpTo).baseClasses
            .mapTo(mutableSetOf()) { it.polymorphicTypeName() }

        // Assert
        assertThat(responseTypes).isNotEmpty()
        assertThat(polymorphicTypes).isNotEmpty()
        assertThat(polymorphicTypes - typesWithFallback).isEmpty()
    }

    private fun responseBodyType(method: Method): Type? {
        val continuation = method.genericParameterTypes.lastOrNull() as? ParameterizedType
        val result = continuation?.takeIf { it.rawType == Continuation::class.java }
            ?.actualTypeArguments?.singleOrNull()
            .let { it as? WildcardType }
            ?.lowerBounds?.singleOrNull() as? ParameterizedType

        return result?.actualTypeArguments?.singleOrNull()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun SerialDescriptor.polymorphicTypeNames(
        visited: MutableSet<SerialDescriptor> = Collections.newSetFromMap(IdentityHashMap()),
    ): Set<String> {
        if (!visited.add(this)) return emptySet()

        val own = polymorphicTypeName()?.takeUnless { it.startsWith(KOTLINX_SERIAL_NAME_PREFIX) }
        return elementDescriptors.flatMapTo(setOfNotNull(own).toMutableSet()) { it.polymorphicTypeNames(visited) }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun SerialDescriptor.polymorphicTypeName(): String? = when (kind) {
        PolymorphicKind.SEALED -> serialName.removeSuffix("?")
        PolymorphicKind.OPEN -> capturedKClass?.qualifiedName
        else -> null
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun KClass<*>.polymorphicTypeName(): String? =
        runCatching { serializer(java).descriptor }.getOrNull()
            ?.takeIf { it.kind == PolymorphicKind.SEALED }
            ?.serialName
            ?: qualifiedName

    private companion object {
        const val KOTLINX_SERIAL_NAME_PREFIX = "kotlinx.serialization."
    }

    @OptIn(ExperimentalSerializationApi::class)
    private class DefaultDeserializerCollector : SerializersModuleCollector {

        val baseClasses = mutableSetOf<KClass<*>>()

        override fun <T : Any> contextual(
            kClass: KClass<T>,
            provider: (typeArgumentsSerializers: List<KSerializer<*>>) -> KSerializer<*>,
        ) = Unit

        override fun <Base : Any, Sub : Base> polymorphic(
            baseClass: KClass<Base>,
            actualClass: KClass<Sub>,
            actualSerializer: KSerializer<Sub>,
        ) = Unit

        override fun <Base : Any> polymorphicDefaultSerializer(
            baseClass: KClass<Base>,
            defaultSerializerProvider: (value: Base) -> SerializationStrategy<Base>?,
        ) = Unit

        override fun <Base : Any> polymorphicDefaultDeserializer(
            baseClass: KClass<Base>,
            defaultDeserializerProvider: (className: String?) -> DeserializationStrategy<Base>?,
        ) {
            baseClasses += baseClass
        }
    }
}