package com.tangem.core.socket

import com.tangem.core.socket.transport.SocketTransport
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * @property headers handshake headers, resolved before every connect attempt
 * @property transport replaces the factory's transport, e.g. an [com.tangem.core.socket.transport.OkHttpSocketTransport] over a client of your own
 * @property lifecycle extra condition for holding the connection, on top of foreground, network and collectors
 * @property reconnectOn every emission after the first reconnects at once without backoff, e.g. rotated credentials;
 * the socket connects without waiting for the first one
 * @property onOpen runs on every (re)connect before the keep-alive starts: auth, the full subscription set. It must only
 * send: incoming frames wait until it returns, and frames sent from outside meanwhile go out right after it
 * @property isSplitArrayFrames delivers every element of a JSON array frame as a separate message
 * @property idleTimeout how long the connection outlives its last collector
 */
data class SocketSpec(
    val url: String,
    val headers: SocketHeaders = SocketHeaders.None,
    val transport: SocketTransport? = null,
    val lifecycle: SocketLifecycle = SocketLifecycle.Always,
    val reconnectOn: Flow<Any?>? = null,
    val keepAlive: KeepAlive = KeepAlive.None,
    val backoff: Backoff = Backoff.exponential(),
    val closePolicy: ClosePolicy = ClosePolicy.AlwaysRetry,
    val onOpen: suspend SocketSession.() -> Unit = {},
    val isSplitArrayFrames: Boolean = false,
    val idleTimeout: Duration = 5.seconds,
)