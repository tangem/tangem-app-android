package com.tangem.domain.accounts.error

sealed class SelectedAccountError {

    data object NoAccountSelected : SelectedAccountError()

    data class DataError(val cause: Throwable) : SelectedAccountError()
}