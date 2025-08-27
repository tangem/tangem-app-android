package com.tangem.features.hotwallet.walletbackup.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.walletbackup.entity.BackupStatus
import com.tangem.features.hotwallet.walletbackup.entity.WalletBackupUM
import com.tangem.features.hotwallet.common.ui.OptionBlock
import com.tangem.core.ui.R
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.resourceReference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WalletBackupContent(state: WalletBackupUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.secondary)
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        AppBarWithBackButton(
            text = stringResourceSafe(R.string.common_backup),
            onBackClick = state.onBackClick,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            OptionBlock(
                modifier = Modifier.fillMaxWidth(),
                title = stringResourceSafe(R.string.hw_backup_seed_title),
                description = stringResourceSafe(R.string.hw_backup_seed_description),
                badge = {
                    state.recoveryPhraseOption?.let { Label(it) }
                },
                onClick = state.onRecoveryPhraseClick,
                enabled = true,
                backgroundColor = TangemTheme.colors.background.primary,
            )

            OptionBlock(
                modifier = Modifier.fillMaxWidth(),
                title = stringResourceSafe(R.string.hw_backup_google_drive_title),
                description = stringResourceSafe(R.string.hw_backup_google_drive_description),
                badge = {
                    state.googleDriveOption?.let { Label(it) }
                },
                onClick = state.onGoogleDriveClick,
                enabled = state.googleDriveStatus != BackupStatus.ComingSoon,
                backgroundColor = TangemTheme.colors.background.primary,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WalletBackupContentPreview(@PreviewParameter(WalletBackupUMProvider::class) state: WalletBackupUM) {
    TangemThemePreview {
        WalletBackupContent(state)
    }
}

private class WalletBackupUMProvider : CollectionPreviewParameterProvider<WalletBackupUM>(
    collection = listOf(
        WalletBackupUM(
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
            backedUp = false,
        ),
        WalletBackupUM(
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
            backedUp = false,
        ),
        WalletBackupUM(
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
            backedUp = false,
        ),
    ),
)