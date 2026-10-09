package com.tangem.core.socket.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeName

internal data class SocketServiceModel(
    val declaration: KSClassDeclaration,
    val visibility: KModifier,
    val methods: List<SocketMethod>,
)

internal sealed interface SocketMethod {

    val name: String

    /** @property decodeAs the sealed base a subtype is decoded through, so the discriminator picks the type */
    data class Receive(override val name: String, val type: TypeName, val decodeAs: TypeName? = null) : SocketMethod

    data class State(override val name: String, val isStateFlow: Boolean) : SocketMethod

    data class Send(override val name: String, val parameterName: String, val type: TypeName) : SocketMethod
}