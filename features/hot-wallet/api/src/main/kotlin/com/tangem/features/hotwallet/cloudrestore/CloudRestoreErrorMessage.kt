package com.tangem.features.hotwallet.cloudrestore

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.cloudbackup.models.CloudBackupError

/** [isWarning] distinguishes an account without backups from a failure to list them. */
data class CloudRestoreErrorMessage(
    val title: TextReference,
    val body: TextReference,
    val isWarning: Boolean,
) {

    companion object {

        private val serviceName = resourceReference(R.string.hw_cloud_backup_service_name)

        /** Null for [CloudBackupError.AuthCanceled]: the viewer left the account picker, there is nothing to tell. */
        fun of(error: CloudBackupError): CloudRestoreErrorMessage? = when (error) {
            CloudBackupError.AuthCanceled -> null
            CloudBackupError.BackupNotFound -> CloudRestoreErrorMessage(
                title = resourceReference(R.string.hw_import_restore_cloud_backup_not_found, wrappedList(serviceName)),
                body = resourceReference(R.string.hw_cloud_backup_no_backups_description),
                isWarning = true,
            )
            CloudBackupError.AuthPermissionsMissing,
            CloudBackupError.AuthRequired,
            -> CloudRestoreErrorMessage(
                title = resourceReference(R.string.hw_cloud_backup_permissions_title_v2, wrappedList(serviceName)),
                body = resourceReference(R.string.hw_cloud_backup_permissions_description_v2, wrappedList(serviceName)),
                isWarning = false,
            )
            CloudBackupError.NetworkError -> CloudRestoreErrorMessage(
                title = resourceReference(R.string.hw_cloud_backup_error_title),
                body = resourceReference(R.string.hw_cloud_backup_error_network),
                isWarning = false,
            )
            else -> CloudRestoreErrorMessage(
                title = resourceReference(R.string.hw_cloud_backup_error_title),
                body = resourceReference(R.string.hw_cloud_backup_restore_error_with_recovery),
                isWarning = false,
            )
        }
    }
}