package com.tangem.features.hotwallet.createcloudbackup.model

import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.domain.cloudbackup.models.CloudBackupError

internal data class CloudBackupErrorSpec(val title: TextReference, val body: TextReference, val isRetryable: Boolean)

internal fun cloudBackupErrorSpec(error: CloudBackupError, serviceName: TextReference): CloudBackupErrorSpec {
    val isPermissions = error == CloudBackupError.AuthPermissionsMissing || error == CloudBackupError.AuthRequired
    val title = if (isPermissions) {
        resourceReference(R.string.hw_cloud_backup_permissions_title_v2, wrappedList(serviceName))
    } else {
        resourceReference(R.string.hw_cloud_backup_error_title)
    }
    val body = when (error) {
        CloudBackupError.NetworkError -> resourceReference(R.string.hw_cloud_backup_error_network)
        CloudBackupError.AuthPermissionsMissing,
        CloudBackupError.AuthRequired,
        -> resourceReference(R.string.hw_cloud_backup_permissions_description_v2, wrappedList(serviceName))
        CloudBackupError.CloudUnavailable -> resourceReference(R.string.hw_cloud_backup_error_unavailable)
        else -> resourceReference(R.string.hw_cloud_backup_error_write)
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
    return CloudBackupErrorSpec(title = title, body = body, isRetryable = isRetryable)
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
            title = spec.title
            body = spec.body
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