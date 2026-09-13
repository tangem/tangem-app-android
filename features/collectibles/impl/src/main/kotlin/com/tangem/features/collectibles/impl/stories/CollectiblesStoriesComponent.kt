package com.tangem.features.collectibles.impl.stories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.collectibles.impl.stories.model.CollectiblesStoriesModel
import com.tangem.features.collectibles.impl.stories.ui.CollectiblesStoriesScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class CollectiblesStoriesComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: CollectiblesStoriesModel = getOrCreateModel()

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        CollectiblesStoriesScreen(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext): CollectiblesStoriesComponent
    }
}