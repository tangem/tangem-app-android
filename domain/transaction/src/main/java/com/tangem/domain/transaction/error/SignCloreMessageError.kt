package com.tangem.domain.transaction.error

sealed class SignCloreMessageError {

    data object WalletManagerNotFound : SignCloreMessageError()

    data object MessageSigningNotSupported : SignCloreMessageError()

    data class SigningFailed(val message: String) : SignCloreMessageError()
}