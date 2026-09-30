package com.tangem.utils.serialization

import kotlinx.serialization.modules.SerializersModule

fun interface SerializersModuleProvider {

    fun provide(): SerializersModule
}