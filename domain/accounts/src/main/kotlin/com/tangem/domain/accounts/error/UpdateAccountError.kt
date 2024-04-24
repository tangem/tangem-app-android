package com.tangem.domain.accounts.error

sealed class UpdateAccountError {

    data object NoAccountUpdated : UpdateAccountError()

    data object CannotBeMainAccount : UpdateAccountError()

    data class DataError(val cause: Throwable) : UpdateAccountError()
}