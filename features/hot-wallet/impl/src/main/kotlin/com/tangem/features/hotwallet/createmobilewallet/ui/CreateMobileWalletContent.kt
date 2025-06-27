package com.tangem.features.hotwallet.createmobilewallet.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.createmobilewallet.entity.CreateMobileWalletUM

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateMobileWalletContent(state: CreateMobileWalletUM, modifier: Modifier = Modifier) {
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
            title = TextReference.EMPTY,
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
            Icon(
                modifier = Modifier
                    .fillMaxWidth(),
                painter = painterResource(R.drawable.ic_create_mobile_wallet_56),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        top = 20.dp,
                        end = 16.dp,
                    ),
                // TODO [REDACTED_TASK_KEY] update and extract this string
                text = "Create Mobile Wallet",
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )
            FeatureBlock(
                modifier = Modifier
                    .padding(top = 32.dp),
                // TODO [REDACTED_TASK_KEY] update and extract this string
                title = "Keys are stored in the app",
                // TODO [REDACTED_TASK_KEY] update and extract this string
                description = "Get notified of incoming transactions",
                iconRes = R.drawable.ic_lock_24,
            )
            FeatureBlock(
                modifier = Modifier
                    .padding(top = 24.dp),
                // TODO [REDACTED_TASK_KEY] update and extract this string
                title = "Seed phrase backup",
                // TODO [REDACTED_TASK_KEY] update and extract this string
                description = "Stay up to date with the latest features and news",
                iconRes = R.drawable.ic_settings_24,
            )
        }
        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            text = "Create",
            showProgress = false,
            enabled = true,
            onClick = state.onCreateClick,
        )
    }
}

@Composable
private fun FeatureBlock(title: String, description: String, iconRes: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
    ) {
        Icon(
            modifier = Modifier
                .padding(horizontal = 12.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = TangemTheme.colors.icon.primary1,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            Text(
                text = title,
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                modifier = Modifier
                    .padding(top = 4.dp),
                text = description,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCreateWalletContent() {
    TangemThemePreview {
        CreateMobileWalletContent(
            state = CreateMobileWalletUM(
                onBackClick = {},
                onCreateClick = {},
            ),
        )
    }
}