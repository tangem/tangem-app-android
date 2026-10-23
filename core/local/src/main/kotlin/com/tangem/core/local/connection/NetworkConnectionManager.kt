package com.tangem.core.local.connection

import kotlinx.coroutines.flow.Flow

/** Network connection manager */
interface NetworkConnectionManager {

    /** Connection status */
    val isOnlineSync: Boolean

    val isOnline: Flow<Boolean>
}