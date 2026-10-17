package com.tangem.core.socket

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

fun interface SocketLifecycle {

    fun isActive(): Flow<Boolean>

    companion object {

        val Always: SocketLifecycle = SocketLifecycle { flowOf(true) }
    }
}

infix fun SocketLifecycle.and(other: SocketLifecycle): SocketLifecycle = SocketLifecycle {
    combine(isActive(), other.isActive()) { first, second -> first && second }.distinctUntilChanged()
}