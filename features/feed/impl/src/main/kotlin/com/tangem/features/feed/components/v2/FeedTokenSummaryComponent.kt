package com.tangem.features.feed.components.v2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.shtorka.ShtorkaExpansion
import com.tangem.features.feed.components.v2.model.FeedTokenSummaryModel
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedScreenComponent
import com.tangem.features.foryou.TokenSummaryComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * [FeedRoute.TokenSummary] screen of the v2 feed: the For You token summary, whose own navigation bar
 * (token icon, title, close) takes the host's sheet header.
 */
@Stable
internal class FeedTokenSummaryComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: FeedRoute.TokenSummary,
    tokenSummaryComponentFactory: TokenSummaryComponent.Factory,
) : FeedScreenComponent, AppComponentContext by context {

    private val model: FeedTokenSummaryModel = getOrCreateModel()

    private val legacyComponent = tokenSummaryComponentFactory.create(
        context = context.child(key = "legacy"),
        params = TokenSummaryComponent.Params(
            token = params.token.toComponentToken(),
            selectedTokenPeriodId = params.selectedTokenPeriodId,
            callbacks = model,
        ),
    )

    @Composable
    override fun Header(sheetExpansion: State<ShtorkaExpansion>, onExpandSheet: () -> Unit, modifier: Modifier) {
        // the v2 header gets no sheet state, and the token summary's title does not depend on it
        val bottomSheetState = remember { mutableStateOf(BottomSheetState.EXPANDED) }

        Box(modifier = modifier) {
            legacyComponent.Title(bottomSheetState = bottomSheetState)
        }
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
        fun create(context: AppComponentContext, params: FeedRoute.TokenSummary): FeedTokenSummaryComponent
    }
}