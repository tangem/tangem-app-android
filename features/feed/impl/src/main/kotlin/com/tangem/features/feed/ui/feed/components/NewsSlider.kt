package com.tangem.features.feed.ui.feed.components

import androidx.compose.runtime.Composable
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.features.feed.ui.feed.state.NewsSliderConfig

@Composable
internal fun NewsSlider(newsSliderConfig: NewsSliderConfig) {
    val isRedesignEnabled = LocalRedesignEnabled.current
    if (isRedesignEnabled) {
        NewsSliderV2(newsSliderConfig)
    } else {
        NewsSliderV1(newsSliderConfig)
    }
}