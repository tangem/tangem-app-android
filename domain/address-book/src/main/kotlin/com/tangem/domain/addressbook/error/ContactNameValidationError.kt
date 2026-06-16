package com.tangem.domain.addressbook.error

import com.tangem.domain.addressbook.model.ContactName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ContactNameValidationError {

    @Serializable
    data class Format(val error: ContactName.Error) : ContactNameValidationError

    /** Another contact in the same wallet already uses this name (case-insensitive). */
    @Serializable
    data object Duplicate : ContactNameValidationError
}