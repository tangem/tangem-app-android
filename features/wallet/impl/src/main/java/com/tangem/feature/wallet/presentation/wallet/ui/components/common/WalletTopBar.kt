package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.WalletTopBarConfig

/**
 * Wallet screen top bar
 *
 * @param config top bar config
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WalletTopBar(config: WalletTopBarConfig) {
    TopAppBar(
        title = {
            Icon(painter = painterResource(id = R.drawable.img_tangem_logo_90_24), contentDescription = null)
        },
        actions = {
            IconButton(onClick = config.onScanCardClick) {
                Icon(painter = painterResource(id = R.drawable.ic_tap_card_24), contentDescription = "Scan card")
            }
            IconButton(onClick = config.onMoreClick) {
                Icon(painter = painterResource(id = R.drawable.ic_more_vertical_24), contentDescription = "More")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TangemTheme.colors.background.secondary,
            titleContentColor = TangemTheme.colors.icon.primary1,
            actionIconContentColor = TangemTheme.colors.icon.primary1,
        ),
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    )
}

@Preview
@Composable
private fun Preview_WalletTopBar_LightTheme() {
    TangemTheme(isDark = false) {
        WalletTopBar(config = WalletPreviewData.walletTopBarConfig)
    }
}

@Preview
@Composable
private fun Preview_WalletTopBar_DarkTheme() {
    TangemTheme(isDark = true) {
        WalletTopBar(config = WalletPreviewData.walletTopBarConfig)
    }
}