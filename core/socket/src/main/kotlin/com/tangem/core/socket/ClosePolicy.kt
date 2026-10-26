package com.tangem.core.socket

import kotlin.time.Duration

fun interface ClosePolicy {

    fun decide(close: SocketClose): Decision

    sealed interface Decision {

        data object Retry : Decision

        data class RetryAfter(val delay: Duration) : Decision {

            init {
                require(!delay.isNegative()) { "Retry delay must not be negative" }
            }
        }

        data object Stop : Decision
    }

    companion object {

        val AlwaysRetry: ClosePolicy = ClosePolicy { Decision.Retry }
    }
}

/**
 * @property code the close code, [SocketCloseCode.ABNORMAL] when the connection dropped without one
 * @property handshakeStatus the HTTP status of a rejected handshake, e.g. 401 or 403
 */
data class SocketClose(
    val code: Int,
    val reason: String,
    val handshakeStatus: Int? = null,
)

object SocketCloseCode {
    const val NORMAL = 1000
    const val ABNORMAL = 1006
}