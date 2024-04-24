package com.tangem.domain.accounts.error

sealed class AccountsError {

    data object NoAccounts : AccountsError()

    data class DataError(val cause: Throwable) : AccountsError()
}