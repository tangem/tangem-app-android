package com.tangem.domain.cloudbackup.analytics

import com.tangem.domain.cloudbackup.models.CloudBackupError
import java.security.MessageDigest

private const val BYTE_MASK = 0xFF

/**
 * Fixed wording rather than a localized string or an exception message, so the values stay groupable in
 * Amplitude. The underlying [Throwable] is left out: it can carry file names, urls or other user data.
 */
fun CloudBackupError.analyticsMessage(): String = when (this) {
    CloudBackupError.AuthCanceled -> "Authorization canceled"
    CloudBackupError.AuthRequired -> "Authorization required"
    CloudBackupError.AuthPermissionsMissing -> "Permissions missing"
    CloudBackupError.CloudUnavailable -> "Cloud unavailable"
    CloudBackupError.NetworkError -> "Network error"
    CloudBackupError.BackupNotFound -> "Backup not found"
    CloudBackupError.WrongPassword -> "Wrong password"
    CloudBackupError.InvalidBackupFile -> "Invalid backup file"
    is CloudBackupError.WriteError -> "Write error"
    is CloudBackupError.ReadError -> "Read error"
    is CloudBackupError.Unknown -> "Unknown error"
}

/**
 * SHA-256 of a wallet id, for the `User Wallet Id` analytics parameter. The raw id identifies the
 * wallet across the whole backend, so analytics only ever sees this one-way digest of it.
 */
fun analyticsWalletId(walletId: String?): String? = walletId?.let { id ->
    MessageDigest.getInstance("SHA-256")
        .digest(id.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and BYTE_MASK) }
}