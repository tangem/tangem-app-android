package com.tangem.features.hotwallet.walletbackup.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.components.rows.NetworkTitle
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.hotwallet.common.ui.ChevronIcon
import com.tangem.features.hotwallet.common.ui.LoaderIcon
import com.tangem.features.hotwallet.common.ui.OptionBlock
import com.tangem.features.hotwallet.walletbackup.entity.BackupStatus
import com.tangem.features.hotwallet.walletbackup.entity.WalletBackupUM

@Suppress("LongMethod")
@Composable
internal fun WalletBackupContent(state: WalletBackupUM, modifier: Modifier = Modifier) {
    TangemTopBarScaffold(
        modifier = modifier,
        topBar = {
            TangemTopNavigation(
                title = resourceReference(R.string.common_backup),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                onBack = state.onBackClick,
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = contentPadding.calculateTopPadding())
                .padding(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                ),
        ) {
            if (state.hardwareWalletOption != null) {
                OptionBlock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    title = stringResourceSafe(R.string.hw_backup_upgrade_title),
                    description = stringResourceSafe(R.string.hw_backup_upgrade_description),
                    badge = { Label(state.hardwareWalletOption) },
                    onClick = state.onHardwareWalletClick,
                    enabled = true,
                    backgroundColor = TangemTheme.colors3.bg.secondary,
                )

                NetworkTitle(
                    modifier = Modifier.padding(top = 8.dp),
                    title = {
                        Text(
                            modifier = Modifier,
                            text = stringResourceSafe(R.string.onboarding_create_wallet_options_button_options),
                            style = TangemTheme.typography3.subheading.medium,
                            color = TangemTheme.colors3.text.tertiary,
                        )
                    },
                )
            }
            OptionBlock(
                modifier = Modifier,
                title = stringResourceSafe(R.string.hw_backup_seed_title),
                description = stringResourceSafe(R.string.hw_backup_seed_description),
                badge = {
                    state.recoveryPhraseOption?.let { Label(it) }
                },
                trailingContent = { ChevronIcon() },
                onClick = state.onRecoveryPhraseClick,
                enabled = true,
                backgroundColor = TangemTheme.colors3.bg.secondary,
            )
            OptionBlock(
                modifier = Modifier
                    .padding(top = 8.dp),
                title = stringResourceSafe(R.string.hw_backup_google_drive_title),
                description = stringResourceSafe(
                    R.string.hw_cloud_backup_cell_description,
                    stringResourceSafe(R.string.hw_cloud_backup_service_name),
                ),
                badge = {
                    state.googleDriveOption?.let { Label(it) }
                },
                trailingContent = googleDriveTrailingContent(state.googleDriveStatus),
                onClick = state.onGoogleDriveClick,
                enabled = state.isGoogleDriveEnabled,
                backgroundColor = TangemTheme.colors3.bg.secondary,
            )
            Spacer(modifier = Modifier.height(16.dp + contentPadding.calculateBottomPadding()))
        }
    }
}

private fun googleDriveTrailingContent(status: BackupStatus): (@Composable () -> Unit)? = when (status) {
    BackupStatus.NetworkError, BackupStatus.ComingSoon -> null
    BackupStatus.Loading -> {
        { LoaderIcon() }
    }
    else -> {
        { ChevronIcon() }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WalletBackupContentPreview(@PreviewParameter(WalletBackupUMProvider::class) state: WalletBackupUM) {
    TangemThemePreviewRedesign {
        WalletBackupContent(state)
    }
}

private class WalletBackupUMProvider : CollectionPreviewParameterProvider<WalletBackupUM>(
    collection = listOf(
        WalletBackupUM(
            hardwareWalletOption = LabelUM(
                text = resourceReference(R.string.common_recommended),
                style = LabelStyle.ACCENT,
            ),
            recoveryPhraseOption = LabelUM(
                text = resourceReference(R.string.hw_backup_no_backup),
                style = LabelStyle.WARNING,
            ),
            googleDriveOption = LabelUM(
                text = resourceReference(R.string.common_coming_soon),
                style = LabelStyle.REGULAR,
            ),
            googleDriveStatus = BackupStatus.ComingSoon,
            onBackClick = {},
            onRecoveryPhraseClick = {},
            onGoogleDriveClick = {},
            onHardwareWalletClick = {},
            isBackedUp = false,
        ),
        WalletBackupUM(
            hardwareWalletOption = LabelUM(
                text = resourceReference(R.string.common_recommended),
                style = LabelStyle.ACCENT,
            ),
            recoveryPhraseOption = LabelUM(
                text = resourceReference(R.string.hw_backup_no_backup),
                style = LabelStyle.WARNING,
            ),
            googleDriveOption = LabelUM(
                text = resourceReference(R.string.hw_backup_no_backup),
                style = LabelStyle.WARNING,
            ),
            googleDriveStatus = BackupStatus.NoBackup,
            onBackClick = {},
            onRecoveryPhraseClick = {},
            onGoogleDriveClick = {},
            onHardwareWalletClick = {},
            isBackedUp = false,
        ),
        WalletBackupUM(
            hardwareWalletOption = LabelUM(
                text = resourceReference(R.string.common_recommended),
                style = LabelStyle.ACCENT,
            ),
            recoveryPhraseOption = LabelUM(
                text = resourceReference(R.string.common_done),
                style = LabelStyle.ACCENT,
            ),
            googleDriveOption = LabelUM(
                text = resourceReference(R.string.common_done),
                style = LabelStyle.ACCENT,
            ),
            googleDriveStatus = BackupStatus.Done,
            onBackClick = {},
            onRecoveryPhraseClick = {},
            onGoogleDriveClick = {},
            onHardwareWalletClick = {},
            isBackedUp = false,
        ),
        WalletBackupUM(
            hardwareWalletOption = null,
            recoveryPhraseOption = LabelUM(
                text = resourceReference(R.string.common_done),
                style = LabelStyle.ACCENT,
            ),
            googleDriveOption = LabelUM(
                text = resourceReference(R.string.common_done),
                style = LabelStyle.ACCENT,
            ),
            googleDriveStatus = BackupStatus.Done,
            onBackClick = {},
            onRecoveryPhraseClick = {},
            onGoogleDriveClick = {},
            onHardwareWalletClick = {},
            isBackedUp = false,
        ),
        WalletBackupUM(
            hardwareWalletOption = null,
            recoveryPhraseOption = LabelUM(
                text = resourceReference(R.string.common_done),
                style = LabelStyle.ACCENT,
            ),
            googleDriveOption = null,
            googleDriveStatus = BackupStatus.Loading,
            onBackClick = {},
            onRecoveryPhraseClick = {},
            onGoogleDriveClick = {},
            onHardwareWalletClick = {},
            isBackedUp = true,
        ),
        WalletBackupUM(
            hardwareWalletOption = null,
            recoveryPhraseOption = LabelUM(
                text = resourceReference(R.string.common_done),
                style = LabelStyle.ACCENT,
            ),
            googleDriveOption = LabelUM(
                text = resourceReference(R.string.hw_cloud_backup_status_action_required),
                style = LabelStyle.ATTENTION,
            ),
            googleDriveStatus = BackupStatus.ActionRequired(BackupStatus.ActionRequired.Reason.NoAccess),
            onBackClick = {},
            onRecoveryPhraseClick = {},
            onGoogleDriveClick = {},
            onHardwareWalletClick = {},
            isBackedUp = true,
        ),
        WalletBackupUM(
            hardwareWalletOption = null,
            recoveryPhraseOption = LabelUM(
                text = resourceReference(R.string.common_done),
                style = LabelStyle.ACCENT,
            ),
            googleDriveOption = LabelUM(
                text = resourceReference(R.string.hw_cloud_backup_status_network_error),
                style = LabelStyle.ATTENTION,
            ),
            googleDriveStatus = BackupStatus.NetworkError,
            onBackClick = {},
            onRecoveryPhraseClick = {},
            onGoogleDriveClick = {},
            onHardwareWalletClick = {},
            isBackedUp = true,
        ),
    ),
)