package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import android.content.res.Configuration
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTopBarConfig

/**
 * Wallet screen top bar
 *
 * @param config component config
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WalletTopBar(config: WalletTopBarConfig) {
    TopAppBar(
        title = {
            Icon(painter = painterResource(id = R.drawable.img_tangem_logo_90_24), contentDescription = null)
        },
        actions = {
            IconButton(onClick = config.onDetailsClick, modifier = Modifier.testTag(MainScreenTestTags.MORE_BUTTON)) {
                Icon(painter = painterResource(id = R.drawable.ic_more_vertical_24), contentDescription = null)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TangemTheme.colors.background.secondary,
            titleContentColor = TangemTheme.colors.icon.primary1,
            actionIconContentColor = TangemTheme.colors.icon.primary1,
        ),
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
        modifier = Modifier.testTag(MainScreenTestTags.TOP_BAR),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_WalletTopBar() {
    TangemThemePreview {
        WalletTopBar(config = WalletPreviewData.topBarConfig)
    }
}