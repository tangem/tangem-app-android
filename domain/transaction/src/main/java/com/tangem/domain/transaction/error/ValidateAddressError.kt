package com.tangem.domain.transaction.error

import arrow.core.Either

typealias AddressValidationResult = Either<AddressValidation.Error, AddressValidation.Success>

sealed class AddressValidation {
    sealed class Success : AddressValidation() {
        data object Valid : Success()
        data object ValidXAddress : Success()
    }

    sealed class Error : AddressValidation() {
        data object AddressInWallet : Error()
        data object InvalidAddress : Error()
        data class DataError(val throwable: Throwable) : Error()
    }
}