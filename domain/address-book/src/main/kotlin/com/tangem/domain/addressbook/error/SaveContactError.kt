package com.tangem.domain.addressbook.error

import com.tangem.domain.transaction.error.AddressValidation
import com.tangem.domain.transaction.error.SignHashesError

sealed interface SaveContactError {

    data class Name(val error: ContactNameValidationError) : SaveContactError

    data class Address(val error: AddressValidation.Error) : SaveContactError

    data class Signing(val error: SignHashesError) : SaveContactError
}