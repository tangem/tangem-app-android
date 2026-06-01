package com.tangem.feature.rating

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.feature.rating.model.RatingModel
import com.tangem.feature.rating.ui.RatingBlock
import com.tangem.features.rating.RatingComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultRatingComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: RatingComponent.Params,
) : RatingComponent, AppComponentContext by appComponentContext {

    private val model: RatingModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        RatingBlock(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : RatingComponent.Factory {
        override fun create(context: AppComponentContext, params: RatingComponent.Params): DefaultRatingComponent
    }
}