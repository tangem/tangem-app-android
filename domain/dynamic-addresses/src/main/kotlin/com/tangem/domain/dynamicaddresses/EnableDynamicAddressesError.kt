package com.tangem.domain.dynamicaddresses

sealed class EnableDynamicAddressesError {

    data object ConflictingCustomTokens : EnableDynamicAddressesError()

    data class ServiceError(val cause: Throwable) : EnableDynamicAddressesError()
}