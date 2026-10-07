package com.tangem.core.socket

import com.tangem.core.socket.internal.DefaultSocketChannel
import com.tangem.core.socket.transport.SocketTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

class DefaultSocketChannelFactory(
    private val transport: SocketTransport,
    private val environment: SocketEnvironment,
    private val json: Json,
    private val scope: CoroutineScope,
) : SocketChannelFactory {

    override fun create(spec: SocketSpec): SocketChannel = DefaultSocketChannel(
        spec = spec,
        transport = transport,
        environment = environment,
        json = json,
        scope = scope,
    )
}