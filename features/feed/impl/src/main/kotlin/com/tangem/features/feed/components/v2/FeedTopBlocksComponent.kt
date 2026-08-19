package com.tangem.features.feed.components.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.ui.v2.FeedTopBlocksContent

/**
 * "Part 1" of the Shtorka 2.0 feed: the horizontally scrollable row of top blocks (For you,
 * All services, promo cards, …). Owns its content end to end so blocks can later become
 * contributed plug-ins without touching [DefaultFeedV2Component].
 */
@Stable
internal class FeedTopBlocksComponent(
    context: AppComponentContext,
) : ComposableContentComponent, AppComponentContext by context {

    @Composable
    override fun Content(modifier: Modifier) {
        // TODO: [TWI-1608] real blocks; static placeholders until then
        FeedTopBlocksContent(
            onForYouClick = { router.push(FeedRoute.ForYou) },
            modifier = modifier,
        )
    }
}