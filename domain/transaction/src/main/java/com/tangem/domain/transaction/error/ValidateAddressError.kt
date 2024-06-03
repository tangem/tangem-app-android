package com.tangem.domain.transaction.error

sealed class ValidateAddressError {
    data object AddressInWallet : ValidateAddressError()
    data object InvalidAddress : ValidateAddressError()
    data class DataError(val throwable: Throwable) : ValidateAddressError()
}