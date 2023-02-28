package com.tangem.datasource.connection

import kotlinx.coroutines.flow.StateFlow

/** Network connection manager */
interface NetworkConnectionManager {

    /** Connection status */
    val connectionStatus: StateFlow<ConnectionStatus>
}