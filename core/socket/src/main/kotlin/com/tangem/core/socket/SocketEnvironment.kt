package com.tangem.core.socket

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface SocketEnvironment {

    fun isForeground(): Flow<Boolean>

    /** @return an id of the default network that has internet, null when offline; a new id reconnects at once */
    fun defaultNetwork(): Flow<String?>

    object AlwaysAvailable : SocketEnvironment {

        override fun isForeground(): Flow<Boolean> = flowOf(true)

        override fun defaultNetwork(): Flow<String?> = flowOf("default")
    }
}