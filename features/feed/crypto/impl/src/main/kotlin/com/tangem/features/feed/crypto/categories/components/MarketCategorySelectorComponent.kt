package com.tangem.features.feed.crypto.categories.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.shtorka.ShtorkaExpansion
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.feed.crypto.categories.MarketCategorySelection
import com.tangem.features.feed.crypto.categories.MarketCategorySelectorRoute
import com.tangem.features.feed.crypto.categories.model.MarketCategorySelectorModel
import com.tangem.features.feed.crypto.categories.ui.MarketCategorySelectorContent
import com.tangem.features.feed.crypto.impl.R
import com.tangem.features.feed.nav.FeedScreenComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class MarketCategorySelectorComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: MarketCategorySelectorRoute,
) : FeedScreenComponent, AppComponentContext by context {

    private val model: MarketCategorySelectorModel = getOrCreateModel(params)

    @Composable
    override fun Header(sheetExpansion: State<ShtorkaExpansion>, onExpandSheet: () -> Unit, modifier: Modifier) {
        TangemTopNavigation(
            title = resourceReference(R.string.market_categories),
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            windowInsets = WindowInsets(0.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            fadeEnabled = false,
            blurBackground = false,
            onClose = router::pop,
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
        val state by model.uiState.collectAsStateWithLifecycle()

        MarketCategorySelectorContent(
            state = state,
            modifier = modifier.padding(contentPadding),
        )
    }

    interface ModelCallback {
        fun onCategorySelect(selection: MarketCategorySelection)
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: MarketCategorySelectorRoute): MarketCategorySelectorComponent
    }
}