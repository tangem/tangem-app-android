package com.tangem.domain.cloudbackup.models

/** Errors of cloud backup storage operations */
sealed interface CloudBackupError {

    /** User cancelled the cloud account authorization flow */
    data object AuthCanceled : CloudBackupError

    /** A token was requested silently (no user interaction), but interactive authorization is required */
    data object AuthRequired : CloudBackupError

    /** Authorization finished, but the permissions required to access the storage were not granted */
    data object AuthPermissionsMissing : CloudBackupError

    /** Cloud backup is not supported on the device (no Google services) */
    data object CloudUnavailable : CloudBackupError

    /** Could not reach the cloud storage because of connectivity issues */
    data object NetworkError : CloudBackupError

    /** No backup file found in the cloud storage */
    data object BackupNotFound : CloudBackupError

    /** The password did not decrypt the backup (wrong password or tampered file) */
    data object WrongPassword : CloudBackupError

    /** The downloaded file is not a valid Tangem backup (unsupported structure or corrupted payload) */
    data object InvalidBackupFile : CloudBackupError

    /** Failed to write the backup file to the cloud storage */
    data class WriteError(val cause: Throwable? = null) : CloudBackupError

    /** Failed to read the backup file from the cloud storage */
    data class ReadError(val cause: Throwable? = null) : CloudBackupError

    data class Unknown(val cause: Throwable? = null) : CloudBackupError
}