package com.tangem.network.common

/**
[REDACTED_AUTHOR]
 */
interface NetworkInternalException

sealed class NetworkException(message: String?) : Throwable(message), NetworkInternalException {
}