package com.tangem.domain.accounts.error

sealed class CreateAccountError {

    data class NoAccountCreated(val cause: CheckAccountError) : CreateAccountError()

    data class DataError(val cause: Throwable) : CreateAccountError()
}