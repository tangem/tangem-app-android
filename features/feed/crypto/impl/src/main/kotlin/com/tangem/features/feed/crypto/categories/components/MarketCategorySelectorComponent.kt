package com.tangem.features.feed.crypto.categories.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.shtorka.ShtorkaExpansion
import com.tangem.features.feed.crypto.categories.MarketCategorySelectorRoute
import com.tangem.features.feed.crypto.categories.model.MarketCategorySelectorModel
import com.tangem.features.feed.crypto.categories.ui.MarketCategorySelectorContent
import com.tangem.features.feed.nav.FeedScreenComponent
import com.tangem.features.feed.nav.expandingSheetClick
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class MarketCategorySelectorComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: MarketCategorySelectorRoute,
) : FeedScreenComponent, AppComponentContext by context {

    private val model: MarketCategorySelectorModel = getOrCreateModel(params)

    /**
     * The search field takes the whole header — the screen's own title would say "Categories" twice,
     * once here and once over the list.
     *
     * The close button is a sibling rather than [TangemSearch.State.onCloseClick]: the one built into
     * the field is hidden while it is idle and empty, which is exactly the state the user arrives in.
     */
    @Composable
    override fun Header(sheetExpansion: State<ShtorkaExpansion>, onExpandSheet: () -> Unit, modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TangemSearch(
                state = state.search,
                modifier = Modifier.weight(1f),
            )
            TangemButton.Close(
                onClick = expandingSheetClick(sheetExpansion, onExpandSheet) { router.pop() },
            )
        }
    }

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        onExpandSheet: () -> Unit,
        modifier: Modifier,
    ) {
        val state by model.uiState.collectAsStateWithLifecycle()

        MarketCategorySelectorContent(
            state = state,
            modifier = modifier.padding(contentPadding),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: MarketCategorySelectorRoute): MarketCategorySelectorComponent
    }
}