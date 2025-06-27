package com.tangem.features.hotwallet.addexistingwallet.start.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.addexistingwallet.start.entity.AddExistingWalletStartUM

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddExistingWalletStartContent(state: AddExistingWalletStartUM, modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .background(TangemTheme.colors.background.primary)
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TangemTopAppBar(
            modifier = modifier
                .statusBarsPadding(),
            startButton = TopAppBarButtonUM.Back(state.onBackClick),
            title = "Add existing wallet",
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = 16.dp,
                    top = 24.dp,
                    end = 16.dp,
                ),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                // TODO [REDACTED_TASK_KEY] update and extract this string
                text = "Import wallet",
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )
            OptionBlock(
                modifier = Modifier
                    .padding(top = 24.dp),
                // TODO [REDACTED_TASK_KEY] update and extract this string
                title = "Import a recovery phrase",
                // TODO [REDACTED_TASK_KEY] update and extract this string
                description = "Your 12-word seed phrase to recover your wallet",
                badge = null,
                onClick = state.onImportPhraseClick,
                enabled = true,
            )
            OptionBlock(
                // TODO [REDACTED_TASK_KEY] update and extract this string
                title = "Scan a Tangem Wallet",
                // TODO [REDACTED_TASK_KEY] update and extract this string
                description = "Physical cards that securely store your crypto offline.",
                badge = null,
                onClick = state.onScanCardClick,
                enabled = true,
            )
            OptionBlock(
                // TODO [REDACTED_TASK_KEY] update and extract this string
                title = "Import from Google Drive",
                // TODO [REDACTED_TASK_KEY] update and extract this string
                description = "Recover an existing wallet stored in your Google Drive backup",
                badge = {
                    Box(
                        modifier = Modifier
                            .background(
                                color = TangemTheme.colors.field.focused,
                                shape = TangemTheme.shapes.roundedCorners8,
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            // TODO [REDACTED_TASK_KEY] update and extract this string
                            text = "Coming Soon",
                            style = TangemTheme.typography.caption1,
                            color = TangemTheme.colors.text.tertiary,
                        )
                    }
                },
                onClick = null,
                enabled = false,
            )
        }
        BuyTangemWalletBlock(
            onScanClick = state.onBuyCardClick,
        )
    }
}

@Composable
private fun OptionBlock(
    title: String,
    description: String,
    badge: (@Composable () -> Unit)?,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(
                color = TangemTheme.colors.background.secondary,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .padding(16.dp)
            .conditional(onClick != null) {
                onClick?.let { clickableSingle(onClick = it) } ?: Modifier
            },
    ) {
        Row {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                text = title,
                style = TangemTheme.typography.subtitle1,
                color = if (enabled) {
                    TangemTheme.colors.text.primary1
                } else {
                    TangemTheme.colors.text.secondary
                },
            )
            badge?.invoke()
        }
        Text(
            modifier = Modifier
                .padding(top = 4.dp),
            text = description,
            style = TangemTheme.typography.body2,
            color = if (enabled) {
                TangemTheme.colors.text.tertiary
            } else {
                TangemTheme.colors.text.disabled
            },
        )
    }
}

@Composable
private fun BuyTangemWalletBlock(onScanClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = TangemTheme.colors.field.primary,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .padding(
                horizontal = 20.dp,
                vertical = 16.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            // TODO [REDACTED_TASK_KEY] update and extract this string
            text = "Want to purchase a Tangem Wallet?",
            style = TangemTheme.typography.button,
            color = TangemTheme.colors.text.primary1,
        )
        PrimaryButton(
            modifier = Modifier
                .wrapContentWidth(),
            // TODO [REDACTED_TASK_KEY] update and extract this string
            text = "Buy Now",
            onClick = onScanClick,
            size = TangemButtonSize.RoundedAction,
            colors = TangemButtonsDefaults.secondaryButtonColors,
            enabled = true,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCreateWalletContent() {
    TangemThemePreview {
        AddExistingWalletStartContent(
            state = AddExistingWalletStartUM(
                onBackClick = {},
                onImportPhraseClick = {},
                onScanCardClick = {},
                onBuyCardClick = {},
            ),
        )
    }
}