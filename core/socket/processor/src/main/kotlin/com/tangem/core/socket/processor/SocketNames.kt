package com.tangem.core.socket.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

internal object SocketNames {

    private const val SOCKET_PACKAGE = "com.tangem.core.socket"

    const val SOCKET_SERVICE = "$SOCKET_PACKAGE.SocketService"
    const val RECEIVE = "$SOCKET_PACKAGE.Receive"
    const val SEND = "$SOCKET_PACKAGE.Send"
    const val SOCKET_STATE = "$SOCKET_PACKAGE.SocketState"
    const val FLOW = "kotlinx.coroutines.flow.Flow"
    const val STATE_FLOW = "kotlinx.coroutines.flow.StateFlow"
    const val BOOLEAN = "kotlin.Boolean"
    const val SERIALIZABLE = "kotlinx.serialization.Serializable"

    val socketChannel = ClassName(SOCKET_PACKAGE, "SocketChannel")
    val socketChannelFactory = ClassName(SOCKET_PACKAGE, "SocketChannelFactory")
    val socketSpec = ClassName(SOCKET_PACKAGE, "SocketSpec")
    val socketState = ClassName(SOCKET_PACKAGE, "SocketState")
    val flow = ClassName("kotlinx.coroutines.flow", "Flow")
    val stateFlow = ClassName("kotlinx.coroutines.flow", "StateFlow")
    val mutableSharedFlow = ClassName("kotlinx.coroutines.flow", "MutableSharedFlow")
    val mutableStateFlow = ClassName("kotlinx.coroutines.flow", "MutableStateFlow")
    val kSerializer = ClassName("kotlinx.serialization", "KSerializer")
    val serializer = MemberName("kotlinx.serialization", "serializer")
    val filterIsInstance = MemberName("kotlinx.coroutines.flow", "filterIsInstance")
}