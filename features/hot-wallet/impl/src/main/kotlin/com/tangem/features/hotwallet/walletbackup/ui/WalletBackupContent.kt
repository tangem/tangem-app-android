package com.tangem.features.hotwallet.walletbackup.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.tangem.features.hotwallet.common.ui.DISABLED_COLORS_ALPHA

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
                    BackupStatusBadge(status = state.recoveryPhraseStatus)
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
                    BackupStatusBadge(status = state.googleDriveStatus)
                },
                onClick = state.onGoogleDriveClick,
                enabled = state.googleDriveStatus != BackupStatus.ComingSoon,
                backgroundColor = TangemTheme.colors.background.primary,
            )
        }
    }
}

@Composable
private fun BackupStatusBadge(status: BackupStatus, modifier: Modifier = Modifier) {
    val text = when (status) {
        BackupStatus.Done -> stringResourceSafe(R.string.common_done)
        BackupStatus.ComingSoon -> stringResourceSafe(R.string.common_coming_soon)
        BackupStatus.NoBackup -> stringResourceSafe(R.string.hw_backup_no_backup)
    }

    val backgroundColor by animateColorAsState(
        targetValue = when (status) {
            BackupStatus.Done -> TangemTheme.colors.text.accent.copy(alpha = 0.1f)
            BackupStatus.ComingSoon -> TangemTheme.colors.control.unchecked.copy(DISABLED_COLORS_ALPHA)
            BackupStatus.NoBackup -> TangemTheme.colors.text.warning.copy(alpha = 0.1f)
        },
    )

    val textColor by animateColorAsState(
        targetValue = when (status) {
            BackupStatus.Done -> TangemTheme.colors.text.accent
            BackupStatus.ComingSoon -> TangemTheme.colors.text.secondary.copy(DISABLED_COLORS_ALPHA)
            BackupStatus.NoBackup -> TangemTheme.colors.text.warning
        },
    )

    AnimatedContent(targetState = text) { text ->
        Box(
            modifier = modifier
                .padding(horizontal = 4.dp)
                .background(
                    color = backgroundColor,
                    shape = TangemTheme.shapes.roundedCorners8,
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = text,
                style = TangemTheme.typography.caption1,
                color = textColor,
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
            recoveryPhraseStatus = BackupStatus.NoBackup,
            googleDriveStatus = BackupStatus.ComingSoon,
            onBackClick = {},
            onRecoveryPhraseClick = {},
            onGoogleDriveClick = {},
        ),
        WalletBackupUM(
            recoveryPhraseStatus = BackupStatus.NoBackup,
            googleDriveStatus = BackupStatus.NoBackup,
            onBackClick = {},
            onRecoveryPhraseClick = {},
            onGoogleDriveClick = {},
        ),
        WalletBackupUM(
            recoveryPhraseStatus = BackupStatus.Done,
            googleDriveStatus = BackupStatus.Done,
            onBackClick = {},
            onRecoveryPhraseClick = {},
            onGoogleDriveClick = {},
        ),
    ),
)