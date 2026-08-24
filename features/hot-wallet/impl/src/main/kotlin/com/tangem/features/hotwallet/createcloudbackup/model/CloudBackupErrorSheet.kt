package com.tangem.features.hotwallet.createcloudbackup.model

import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.domain.cloudbackup.models.CloudBackupError

internal data class CloudBackupErrorSpec(val titleRes: Int, val bodyRes: Int, val isRetryable: Boolean)

internal fun cloudBackupErrorSpec(error: CloudBackupError): CloudBackupErrorSpec {
    val isPermissions = error == CloudBackupError.AuthPermissionsMissing || error == CloudBackupError.AuthRequired
    val titleRes = if (isPermissions) {
        R.string.hw_cloud_backup_permissions_title
    } else {
        R.string.hw_cloud_backup_error_title
    }
    val bodyRes = when (error) {
        CloudBackupError.NetworkError -> R.string.hw_cloud_backup_error_network
        CloudBackupError.AuthPermissionsMissing,
        CloudBackupError.AuthRequired,
        -> R.string.hw_cloud_backup_permissions_description
        CloudBackupError.CloudUnavailable -> R.string.hw_cloud_backup_error_unavailable
        else -> R.string.hw_cloud_backup_error_write
    }
    val isRetryable = when (error) {
        CloudBackupError.NetworkError,
        is CloudBackupError.WriteError,
        is CloudBackupError.ReadError,
        is CloudBackupError.Unknown,
        CloudBackupError.BackupNotFound,
        -> true
        else -> false
    }
    return CloudBackupErrorSpec(titleRes = titleRes, bodyRes = bodyRes, isRetryable = isRetryable)
}

/** [onClosed] runs only when the sheet is closed without pressing the action button */
internal fun cloudBackupErrorSheet(
    spec: CloudBackupErrorSpec,
    onClosed: () -> Unit = {},
    onAction: () -> Unit,
): BottomSheetMessage {
    var isActionTaken = false

    return bottomSheetMessage {
        onDismiss { if (!isActionTaken) onClosed() }
        infoBlock {
            icon(R.drawable.ic_alert_triangle_20) {
                type = MessageBottomSheetUM.Icon.Type.Warning
                backgroundType = MessageBottomSheetUM.Icon.BackgroundType.SameAsTint
            }
            title = resourceReference(spec.titleRes)
            body = resourceReference(spec.bodyRes)
        }
        primaryButton {
            val buttonRes = if (spec.isRetryable) R.string.hw_cloud_backup_retry else R.string.common_got_it
            text = resourceReference(buttonRes)
            onClick {
                isActionTaken = true
                onAction()
                closeBs()
            }
        }
    }
}