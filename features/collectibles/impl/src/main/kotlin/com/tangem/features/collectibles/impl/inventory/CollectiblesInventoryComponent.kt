package com.tangem.features.collectibles.impl.inventory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.collectibles.impl.inventory.model.CollectiblesInventoryModel
import com.tangem.features.collectibles.impl.inventory.ui.CollectiblesInventoryScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class CollectiblesInventoryComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: CollectiblesInventoryModel = getOrCreateModel()

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val pullToRefresh = remember(state.isRefreshing) {
            PullToRefreshConfig(isRefreshing = state.isRefreshing, onRefresh = { model.onRefresh() })
        }

        CollectiblesInventoryScreen(
            state = state,
            pullToRefresh = pullToRefresh,
            onCloseClick = router::pop,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext): CollectiblesInventoryComponent
    }
}