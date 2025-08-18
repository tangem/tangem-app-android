package com.tangem.features.hotwallet.addexistingwallet.start.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.addexistingwallet.start.entity.AddExistingWalletStartUM
import com.tangem.features.hotwallet.common.ui.OptionBlock

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddExistingWalletStartContent(state: AddExistingWalletStartUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TangemTopAppBar(
            modifier = Modifier
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
                text = stringResourceSafe(R.string.wallet_import_seed_navtitle),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )
            OptionBlock(
                modifier = Modifier
                    .padding(top = 24.dp),
                backgroundColor = TangemTheme.colors.background.secondary,
                title = stringResourceSafe(R.string.wallet_import_seed_title),
                description = stringResourceSafe(R.string.wallet_import_seed_description),
                badge = null,
                onClick = state.onImportPhraseClick,
                enabled = true,
            )
            OptionBlock(
                backgroundColor = TangemTheme.colors.background.secondary,
                title = stringResourceSafe(R.string.wallet_import_scan_title),
                description = stringResourceSafe(R.string.wallet_import_scan_description),
                badge = {
                    if (state.isScanInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(20.dp)
                                .padding(2.dp),
                            color = TangemTheme.colors.text.primary1,
                            strokeWidth = TangemTheme.dimens.size2,
                        )
                    } else {
                        Icon(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(20.dp),
                            painter = painterResource(R.drawable.ic_tangem_24),
                            contentDescription = null,
                            tint = TangemTheme.colors.icon.secondary,
                        )
                    }
                },
                onClick = state.onScanCardClick,
                enabled = true,
            )
            OptionBlock(
                backgroundColor = TangemTheme.colors.background.secondary,
                title = stringResourceSafe(R.string.wallet_import_google_drive_title),
                description = stringResourceSafe(R.string.wallet_import_google_drive_description),
                badge = {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .background(
                                color = TangemTheme.colors.field.focused,
                                shape = TangemTheme.shapes.roundedCorners8,
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResourceSafe(R.string.common_coming_soon),
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
            text = stringResourceSafe(R.string.wallet_import_buy_question),
            style = TangemTheme.typography.button,
            color = TangemTheme.colors.text.primary1,
        )
        PrimaryButton(
            modifier = Modifier
                .wrapContentWidth(),
            text = stringResourceSafe(R.string.wallet_import_buy_title),
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
                isScanInProgress = true,
                onBackClick = {},
                onImportPhraseClick = {},
                onScanCardClick = {},
                onBuyCardClick = {},
            ),
        )
    }
}