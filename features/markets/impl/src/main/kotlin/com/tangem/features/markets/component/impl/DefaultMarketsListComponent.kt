package com.tangem.features.markets.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.markets.component.BottomSheetState
import com.tangem.features.markets.component.MarketsListComponent
import com.tangem.features.markets.model.MarketsListModel
import com.tangem.features.markets.ui.MarketsList
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultMarketsListComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
) : MarketsListComponent, AppComponentContext by context {

    private val model: MarketsListModel = getOrCreateModel()

    @Composable
    override fun BottomSheetContent(
        bottomSheetState: State<BottomSheetState>,
        onHeaderSizeChange: (Dp) -> Unit,
        modifier: Modifier,
    ) {
        val state by model.state.collectAsStateWithLifecycle()
        val bsState by bottomSheetState

        LaunchedEffect(bsState) {
            model.containerBottomSheetState.value = bsState
        }

        MarketsList(
            modifier = modifier,
            state = state,
            onHeaderSizeChange = onHeaderSizeChange,
            bottomSheetState = bsState,
        )
    }

    @AssistedFactory
    interface Factory : MarketsListComponent.Factory {
        override fun create(context: AppComponentContext): DefaultMarketsListComponent
    }
}