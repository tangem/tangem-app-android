package com.tangem.features.feed.components.v2.blocks

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.navigation.pushZooming
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.utils.zoomTransitionSource
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.v2.FeedBlockShape
import com.tangem.features.feed.ui.v2.FeedShortcutCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Shortcut into the All Services screen. Takes the Add Account block's place while All Services is enabled. */
internal class ServicesBlockComponent(
    context: AppComponentContext,
) : FeedBlockComponent(context = context, key = KEY) {

    override val size: FeedBlock.Size = FeedBlock.Size.Small

    override val isVisible: StateFlow<Boolean> = MutableStateFlow(true)

    @Composable
    override fun Content(modifier: Modifier) {
        FeedShortcutCard(
            title = stringResourceSafe(R.string.all_services_title),
            onClick = { router.pushZooming(route = AppRoute.AllServices, sourceKey = SHARED_TRANSITION_KEY) },
            modifier = modifier.zoomTransitionSource(key = SHARED_TRANSITION_KEY, shape = FeedBlockShape),
            icon = { FeedPlusIcon() },
        )
    }

    companion object {
        const val KEY = "services"
        const val SHARED_TRANSITION_KEY = "feed_services_block"
    }
}