package com.tangem.core.socket

import kotlin.time.Duration

sealed interface SocketState {

    data object Idle : SocketState

    data class Connecting(val attempt: Int) : SocketState

    data object Connected : SocketState

    data class WaitingToRetry(val attempt: Int, val delay: Duration) : SocketState

    data class Paused(val reason: PauseReason) : SocketState

    data class Stopped(val code: Int, val reason: String) : SocketState

    enum class PauseReason { Background, NoNetwork }
}