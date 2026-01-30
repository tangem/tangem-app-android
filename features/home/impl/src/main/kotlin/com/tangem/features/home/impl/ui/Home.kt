package com.tangem.features.home.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.SystemBarsIconsDisposable
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.utils.ChangeRootBackgroundColorEffect
import com.tangem.features.home.impl.ui.compose.StoriesScreenV2
import com.tangem.features.home.impl.ui.state.HomeUM

@Composable
internal fun Home(state: HomeUM, modifier: Modifier = Modifier) {
    SystemBarsIconsDisposable(darkIcons = false)

    StoriesScreenV2(
        modifier = modifier,
        state = state,
        onGetStartedClick = state.onGetStartedClick,
    )

    ChangeRootBackgroundColorEffect(TangemColorPalette.Black)
}