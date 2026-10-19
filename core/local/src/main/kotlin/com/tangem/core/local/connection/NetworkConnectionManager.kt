package com.tangem.core.local.connection

import kotlinx.coroutines.flow.Flow

/** Network connection manager */
interface NetworkConnectionManager {

    /** Connection status */
    val isOnline: Boolean

    val isOnlineFlow: Flow<Boolean>
}