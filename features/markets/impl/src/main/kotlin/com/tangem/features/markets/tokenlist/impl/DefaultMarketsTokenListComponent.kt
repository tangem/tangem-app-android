package com.tangem.features.markets.tokenlist.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.domain.markets.TokenMarket
import com.tangem.features.markets.component.BottomSheetState
import com.tangem.features.markets.tokenlist.api.MarketsTokenListComponent
import com.tangem.features.markets.tokenlist.impl.model.MarketsListModel
import com.tangem.features.markets.tokenlist.impl.ui.MarketsList
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DefaultMarketsTokenListComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val onTokenSelected: (TokenMarket) -> Unit,
) : AppComponentContext by appComponentContext, MarketsTokenListComponent {

    private val model: MarketsListModel = getOrCreateModel()

    init {
        model.tokenSelected
            .onEach { onTokenSelected(it) }
            .launchIn(componentScope)
    }

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
    interface Factory : MarketsTokenListComponent.Factory {
        override fun create(
            context: AppComponentContext,
            onTokenSelected: (TokenMarket) -> Unit,
        ): DefaultMarketsTokenListComponent
    }
}
