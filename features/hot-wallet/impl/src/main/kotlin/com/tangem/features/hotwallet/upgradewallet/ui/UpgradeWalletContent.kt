package com.tangem.features.hotwallet.upgradewallet.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.upgradewallet.entity.UpgradeWalletUM

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UpgradeWalletContent(state: UpgradeWalletUM, modifier: Modifier = Modifier) {
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
                painter = painterResource(R.drawable.ic_tangem_64),
                contentDescription = null,
                tint = Color.Unspecified,
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        top = 20.dp,
                        end = 16.dp,
                    ),
                text = stringResourceSafe(R.string.hw_upgrade_title),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )
            FeatureBlock(
                modifier = Modifier
                    .padding(top = 32.dp),
                title = stringResourceSafe(R.string.hw_upgrade_key_migration_title),
                description = stringResourceSafe(R.string.hw_upgrade_key_migration_description),
                iconRes = R.drawable.ic_mobile_security_24,
            )
            FeatureBlock(
                modifier = Modifier
                    .padding(top = 24.dp),
                title = stringResourceSafe(R.string.hw_upgrade_funds_access_title),
                description = stringResourceSafe(R.string.hw_upgrade_funds_access_description),
                iconRes = R.drawable.ic_knight_shield_24,
            )
            FeatureBlock(
                modifier = Modifier
                    .padding(top = 24.dp),
                title = stringResourceSafe(R.string.hw_upgrade_general_security_title),
                description = stringResourceSafe(R.string.hw_upgrade_general_security_description),
                iconRes = R.drawable.ic_protect_24,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SecondaryButton(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResourceSafe(R.string.details_buy_wallet),
                onClick = state.onBuyTangemWalletClick,
            )
            PrimaryButtonIconEnd(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResourceSafe(R.string.hw_upgrade_scan_device),
                onClick = state.onScanDeviceClick,
                iconResId = R.drawable.ic_tangem_24,
            )
        }
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
private fun PreviewUpgradeWalletContent() {
    TangemThemePreview {
        UpgradeWalletContent(
            state = UpgradeWalletUM(
                onBackClick = {},
                onBuyTangemWalletClick = {},
                onScanDeviceClick = {},
            ),
        )
    }
}