package com.tangem.core.socket

fun interface SocketHeaders {

    /** A failure here ends the attempt like a dropped connection: [ClosePolicy] decides what comes next. */
    suspend fun resolve(context: SocketConnectContext): Map<String, String>

    companion object {

        val None: SocketHeaders = SocketHeaders { emptyMap() }
    }
}

/**
 * @property attempt attempts since the last healthy connection or gate change: 0 for the first one, so a reconnect
 * after a healthy connection is 1
 * @property previousClose how the previous attempt ended; a [SocketClose.handshakeStatus] of 401 means the token
 * the headers carried was rejected
 */
data class SocketConnectContext(
    val attempt: Int,
    val previousClose: SocketClose?,
)