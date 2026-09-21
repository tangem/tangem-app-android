package com.tangem.features.collectibles.impl.machinedetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.collectibles.impl.machinedetails.model.CollectiblesMachineDetailsModel
import com.tangem.features.collectibles.impl.machinedetails.ui.CollectiblesMachineDetailsScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class CollectiblesMachineDetailsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: CollectiblesMachineDetailsModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        CollectiblesMachineDetailsScreen(state = state, onLoadMore = model::onLoadMore, modifier = modifier)
    }

    data class Params(val externalId: String)

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): CollectiblesMachineDetailsComponent
    }
}