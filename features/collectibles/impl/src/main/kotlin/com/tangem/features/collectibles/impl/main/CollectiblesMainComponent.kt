package com.tangem.features.collectibles.impl.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.collectibles.impl.main.model.CollectiblesMainModel
import com.tangem.features.collectibles.impl.main.ui.CollectiblesMainScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/** The Collectibles catalogue: balance, actions and the packs on sale. */
internal class CollectiblesMainComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: CollectiblesMainModel = getOrCreateModel()

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        CollectiblesMainScreen(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext): CollectiblesMainComponent
    }
}