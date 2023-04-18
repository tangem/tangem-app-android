package com.tangem.datasource.connection

import kotlinx.coroutines.flow.StateFlow

/** Network connection manager */
interface NetworkConnectionManager {

    /** Connection status */
    val isOnline: Boolean

    /** Connection status flow */
    val isOnlineFlow: StateFlow<Boolean>
}
