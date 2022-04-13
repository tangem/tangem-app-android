package com.tangem.domain

/**
[REDACTED_AUTHOR]
 * Must be handled by the module or sent to Crashlytics
 */
interface DomainInternalException

sealed class DomainException(message: String?) : Throwable(message), DomainInternalException {
    data class SelectTokeNetworkException(val networkId: String) : DomainException(
        "Unknown network [$networkId] should not be included in the network selection dialog."
    )
}