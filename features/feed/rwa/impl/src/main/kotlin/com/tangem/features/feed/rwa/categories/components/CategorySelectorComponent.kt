package com.tangem.features.feed.rwa.categories.components

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
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.shtorka.ShtorkaExpansion
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.feed.nav.FeedScreenComponent
import com.tangem.features.feed.nav.expandingSheetClick
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.CategorySelectorRoute
import com.tangem.features.feed.rwa.categories.model.CategorySelectorModel
import com.tangem.features.feed.rwa.categories.ui.CategorySelectorContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class CategorySelectorComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: CategorySelectorRoute,
) : FeedScreenComponent, AppComponentContext by context {

    private val model: CategorySelectorModel = getOrCreateModel(params)

    @Composable
    override fun Header(sheetExpansion: State<ShtorkaExpansion>, onExpandSheet: () -> Unit, modifier: Modifier) {
        TangemTopNavigation(
            title = resourceReference(R.string.markets_real_assets_filter_title),
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            windowInsets = WindowInsets(0.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            fadeEnabled = false,
            blurBackground = false,
            onClose = expandingSheetClick(sheetExpansion, onExpandSheet) { router.pop() },
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

        CategorySelectorContent(
            state = state,
            modifier = modifier.padding(contentPadding),
        )
    }

    interface ModelCallback {
        fun onCategorySelect(selection: CategorySelection)
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: CategorySelectorRoute): CategorySelectorComponent
    }
}