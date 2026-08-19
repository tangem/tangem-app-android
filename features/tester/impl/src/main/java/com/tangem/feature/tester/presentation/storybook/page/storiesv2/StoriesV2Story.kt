package com.tangem.feature.tester.presentation.storybook.page.storiesv2

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The story player showcase. The page owns the whole screen because the player is full-bleed and manages its own
 * insets — a story with the system bars padded away is not the thing that ships.
 */
@Composable
internal fun StoriesV2Story(modifier: Modifier = Modifier) {
    StoriesV2DemoHost(modifier = modifier.fillMaxSize())
}