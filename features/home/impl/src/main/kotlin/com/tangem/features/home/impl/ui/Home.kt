package com.tangem.features.home.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.SystemBarsIconsDisposable
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.utils.ChangeRootBackgroundColorEffect
import com.tangem.features.home.impl.ui.compose.StoriesScreen
import com.tangem.features.home.impl.ui.compose.StoriesScreenV2
import com.tangem.features.home.impl.ui.state.HomeUM

@Composable
internal fun Home(state: HomeUM, isV2StoriesEnabled: Boolean, modifier: Modifier = Modifier) {
    SystemBarsIconsDisposable(darkIcons = false)

    if (isV2StoriesEnabled) {
        StoriesScreenV2(
            modifier = modifier,
            state = state,
            onCreateNewWalletButtonClick = state.onCreateNewWalletClick,
            onAddExistingWalletButtonClick = state.onAddExistingWalletClick,
            onScanButtonClick = state.onScanClick,
        )
    } else {
        StoriesScreen(
            modifier = modifier,
            state = state,
            onScanButtonClick = state.onScanClick,
            onShopButtonClick = state.onShopClick,
            onSearchTokensClick = state.onSearchTokensClick,
        )
    }

    ChangeRootBackgroundColorEffect(TangemColorPalette.Black)
}