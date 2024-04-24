package com.tangem.domain.accounts.error

sealed class CheckAccountError {

    data object AlreadyCreated : CheckAccountError()

    data object CannotBeMainAccount : CheckAccountError()

    data class WrongId(val cause: Throwable) : CheckAccountError()

    data class WrongTitle(val cause: Throwable) : CheckAccountError()

    data class DataError(val cause: Throwable) : CheckAccountError()
}