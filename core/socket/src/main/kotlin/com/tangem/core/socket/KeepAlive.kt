package com.tangem.core.socket

import kotlin.time.Duration

sealed interface KeepAlive {

    data object None : KeepAlive

    /** The first [text] goes out a full [interval] after [SocketSpec.onOpen], never before it. */
    data class ClientSends(val text: String, val interval: Duration) : KeepAlive {

        init {
            require(interval.isPositive()) { "Keep-alive interval must be positive" }
        }
    }

    data class ServerSends(val text: String, val reply: String) : KeepAlive
}