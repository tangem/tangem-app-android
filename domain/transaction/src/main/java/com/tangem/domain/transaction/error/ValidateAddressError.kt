package com.tangem.domain.transaction.error

import arrow.core.Either
import com.tangem.utils.annotations.RemoveWithToggle

typealias AddressValidationResult = Either<AddressValidation.Error, AddressValidation.Success>

sealed class AddressValidation {
    sealed class Success : AddressValidation() {
        data object Valid : Success()
        data object ValidXAddress : Success()
        data class ValidNamedAddress(val blockchainAddress: String) : Success()
    }

    sealed class Error : AddressValidation() {
        data object AddressInWallet : Error()
        data object InvalidAddress : Error()

        @RemoveWithToggle("TWI_1741_TOP_UP_WARNING_ENABLED")
        data object RecipientWalletBackupError : Error()
        data class DataError(val throwable: Throwable) : Error()
    }
}