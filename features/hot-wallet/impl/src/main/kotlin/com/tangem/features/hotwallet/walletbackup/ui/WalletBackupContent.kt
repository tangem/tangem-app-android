package com.tangem.features.hotwallet.walletbackup.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.components.rows.NetworkTitle
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.ForceDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.common.ui.OptionBlock
import com.tangem.features.hotwallet.walletbackup.entity.BackupStatus
import com.tangem.features.hotwallet.walletbackup.entity.WalletBackupUM

@Suppress("LongMethod")
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
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                ),
        ) {
            Banner(state)

            OptionBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                title = stringResourceSafe(R.string.hw_backup_hardware_title),
                description = stringResourceSafe(R.string.hw_backup_hardware_description),
                badge = {
                    state.hardwareWalletOption?.let { Label(it) }
                },
                onClick = state.onHardwareWalletClick,
                enabled = true,
                backgroundColor = TangemTheme.colors.background.primary,
            )
            NetworkTitle(
                modifier = Modifier
                    .padding(top = 8.dp),
                title = {
                    Text(
                        modifier = Modifier,
                        text = stringResourceSafe(R.string.onboarding_create_wallet_options_button_options),
                        style = TangemTheme.typography.subtitle2,
                        color = TangemTheme.colors.text.tertiary,
                    )
                },
            )
            OptionBlock(
                modifier = Modifier,
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
                modifier = Modifier
                    .padding(top = 8.dp),
                title = stringResourceSafe(R.string.hw_backup_google_drive_title),
                description = stringResourceSafe(R.string.hw_backup_google_drive_description),
                badge = {
                    state.googleDriveOption?.let { Label(it) }
                },
                onClick = state.onGoogleDriveClick,
                enabled = state.googleDriveStatus != BackupStatus.ComingSoon,
                backgroundColor = TangemTheme.colors.background.primary,
            )
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

@Suppress("LongMethod")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Banner(state: WalletBackupUM, modifier: Modifier = Modifier) {
    ForceDarkTheme {
        Column(
            modifier = modifier
                .background(
                    color = TangemTheme.colors.background.primary,
                    shape = RoundedCornerShape(16.dp),
                ),
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        start = 12.dp,
                        top = 20.dp,
                        end = 12.dp,
                    ),
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = "Tangem Wallet",
                    style = TangemTheme.typography.h3,
                    color = TangemTheme.colors.text.primary1,
                    textAlign = TextAlign.Center,
                )

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 4.dp,
                        ),
                    text = "Keeps your crypto safe and offline. Slim as a credit card, safer than a bank vault.",
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.tertiary,
                    textAlign = TextAlign.Center,
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 24.dp,
                            top = 16.dp,
                            end = 24.dp,
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FeatureItem(
                        iconResId = R.drawable.ic_shield_check_16,
                        text = resourceReference(R.string.welcome_create_wallet_feature_class),
                    )
                    FeatureItem(
                        iconResId = R.drawable.ic_flash_16,
                        text = resourceReference(R.string.welcome_create_wallet_feature_delivery),
                    )
                    FeatureItem(
                        iconResId = R.drawable.ic_sparkles_16,
                        text = resourceReference(R.string.welcome_create_wallet_feature_use),
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(
                            start = 8.dp,
                            top = 12.dp,
                            end = 8.dp,
                            bottom = 20.dp,
                        ),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_tangem_cards_vertical),
                        contentDescription = null,
                    )
                    SecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        text = stringResourceSafe(R.string.details_buy_wallet),
                        onClick = state.onBuyClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(@DrawableRes iconResId: Int, text: TextReference) {
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(iconResId),
            tint = TangemTheme.colors.icon.accent,
            contentDescription = null,
        )
        Text(
            text = text.resolveReference(),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.secondary,
        )
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
            onBuyClick = {},
            onRecoveryPhraseClick = {},
            onGoogleDriveClick = {},
            onHardwareWalletClick = {},
            backedUp = false,
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
            onBuyClick = {},
            onRecoveryPhraseClick = {},
            onGoogleDriveClick = {},
            onHardwareWalletClick = {},
            backedUp = false,
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
            onBuyClick = {},
            onRecoveryPhraseClick = {},
            onGoogleDriveClick = {},
            onHardwareWalletClick = {},
            backedUp = false,
        ),
    ),
)