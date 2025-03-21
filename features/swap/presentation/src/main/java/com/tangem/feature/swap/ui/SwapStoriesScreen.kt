package com.tangem.feature.swap.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.components.stories.StoriesContainer
import com.tangem.feature.swap.models.SwapStoriesContentConfig

@Composable
internal fun SwapStoriesScreen(config: SwapStoriesContentConfig) {
    StoriesContainer(
        config = config,
    ) { current, _ ->
        // todo stories AND-9353 fix when design is ready
        Image(
            painter = painterResource(current.imageRes),
            contentDescription = null,
        )
    }
}
