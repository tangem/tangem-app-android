package com.tangem.features.feed.components.v2

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.shtorka.ShtorkaExpansion
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.feed.components.v2.model.FeedForYouModel
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedScreenComponent
import com.tangem.features.feed.nav.expandingSheetClick
import com.tangem.features.foryou.ForYouComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * [FeedRoute.ForYou] screen of the v2 feed: the existing For You content topped with the DS3
 * [TangemTopNavigation] instead of the legacy sheet-header title. The legacy callbacks are
 * handled by [FeedForYouModel] — no lambdas cross the route.
 */
@Stable
internal class FeedForYouComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Suppress("UnusedPrivateProperty") @Assisted params: FeedRoute.ForYou,
    forYouComponentFactory: ForYouComponent.Factory,
) : FeedScreenComponent, AppComponentContext by context {

    private val model: FeedForYouModel = getOrCreateModel()

    private val legacyComponent = forYouComponentFactory.create(
        context = context.child(key = "legacy"),
        params = ForYouComponent.Params(callbacks = model),
    )

    @Composable
    override fun Header(sheetExpansion: State<ShtorkaExpansion>, onExpandSheet: () -> Unit, modifier: Modifier) {
        TangemTopNavigation(
            title = resourceReference(R.string.for_you_title),
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            windowInsets = WindowInsets(0.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            fadeEnabled = false,
            blurBackground = false,
            onBack = expandingSheetClick(sheetExpansion, onExpandSheet) { router.pop() },
            modifier = modifier,
        )
    }

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        onExpandSheet: () -> Unit,
        modifier: Modifier,
    ) {
        legacyComponent.Content(
            bottomSheetState = bottomSheetState,
            contentPadding = contentPadding,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: FeedRoute.ForYou): FeedForYouComponent
    }
}