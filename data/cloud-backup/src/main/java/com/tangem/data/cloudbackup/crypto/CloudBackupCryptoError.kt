package com.tangem.data.cloudbackup.crypto

internal sealed interface CloudBackupCryptoError {

    data object WrongPassword : CloudBackupCryptoError

    data class InvalidFormat(val message: String) : CloudBackupCryptoError
}