package com.tangem.domain.cloudbackup.analytics

import com.tangem.domain.cloudbackup.models.CloudBackupError

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