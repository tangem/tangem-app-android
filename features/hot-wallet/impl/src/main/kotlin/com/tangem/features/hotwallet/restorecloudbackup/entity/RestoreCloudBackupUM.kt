package com.tangem.features.hotwallet.restorecloudbackup.entity

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

/**
 * UI state of the restore-cloud-backup flow, rendered by a single `Content()` that switches on the step.
 *
 * The authoritative password is a [CharArray] inside
 * [com.tangem.features.hotwallet.restorecloudbackup.model.RestoreCloudBackupModel]. [EnterPassword] carries
 * the editable text only as a [String] because Compose text fields must render it; its [toString] is
 * redacted so an accidentally-logged UM never leaks the password.
 *
 * [accountEmail] is the cloud account the backups were loaded from, shown as the screen subtitle.
 */
@Immutable
internal sealed interface RestoreCloudBackupUM {

    val accountEmail: String?
    val onBack: () -> Unit

    data class BackupList(
        val items: ImmutableList<BackupRowUM>,
        override val accountEmail: String?,
        override val onBack: () -> Unit,
    ) : RestoreCloudBackupUM

    data class EnterPassword(
        val walletName: String,
        val createdAtMillis: Long,
        val password: String,
        val isPasswordVisible: Boolean,
        val isError: Boolean,
        val isLoading: Boolean,
        val onPasswordChange: (String) -> Unit,
        val onToggleVisibility: () -> Unit,
        val onRestoreClick: () -> Unit,
        override val accountEmail: String?,
        override val onBack: () -> Unit,
    ) : RestoreCloudBackupUM {

        val isRestoreEnabled: Boolean = password.isNotEmpty()

        override fun toString(): String =
            "EnterPassword(walletName=$walletName, isPasswordVisible=$isPasswordVisible, " +
                "isError=$isError, isLoading=$isLoading)"
    }

    /**

     * not stored in the backup, and it cannot be validated here — a typo silently derives a different
     * wallet, exactly as in the seed-phrase import flow.
     */
    data class EnterPassphrase(
        val passphrase: String,
        val isPassphraseVisible: Boolean,
        val isLoading: Boolean,
        val onPassphraseChange: (String) -> Unit,
        val onToggleVisibility: () -> Unit,
        val onContinueClick: () -> Unit,
        override val accountEmail: String?,
        override val onBack: () -> Unit,
    ) : RestoreCloudBackupUM {

        val isContinueEnabled: Boolean = passphrase.isNotEmpty()

        override fun toString(): String =
            "EnterPassphrase(isPassphraseVisible=$isPassphraseVisible, isLoading=$isLoading)"
    }
}

internal data class BackupRowUM(
    val walletName: String,
    val createdAtMillis: Long,
    val onClick: () -> Unit,
)