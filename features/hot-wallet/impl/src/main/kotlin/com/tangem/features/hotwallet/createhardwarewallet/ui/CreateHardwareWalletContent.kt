package com.tangem.features.hotwallet.createhardwarewallet.ui

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
import com.tangem.core.ui.components.feature.FeatureBlock
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.createhardwarewallet.entity.CreateHardwareWalletUM

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateHardwareWalletContent(state: CreateHardwareWalletUM, modifier: Modifier = Modifier) {
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
                text = stringResourceSafe(R.string.wallet_create_common_title),
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
                text = stringResourceSafe(R.string.home_button_scan),
                onClick = state.onScanDeviceClick,
                iconResId = R.drawable.ic_tangem_24,
                showProgress = state.isScanInProgress,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCreateHardwareWalletContent() {
    TangemThemePreview {
        CreateHardwareWalletContent(
            state = CreateHardwareWalletUM(
                onBackClick = {},
                onBuyTangemWalletClick = {},
                onScanDeviceClick = {},
            ),
        )
    }
}