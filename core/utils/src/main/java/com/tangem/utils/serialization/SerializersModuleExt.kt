package com.tangem.utils.serialization

import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

fun Iterable<SerializersModuleProvider>.aggregate(): SerializersModule = SerializersModule {
    forEach { provider -> include(provider.provide()) }
}

inline fun <reified Base : Any> polymorphicSerializersModule(
    noinline builderAction: PolymorphicModuleBuilder<Base>.() -> Unit,
): SerializersModule = SerializersModule {
    polymorphic(baseClass = Base::class, builderAction = builderAction)
}