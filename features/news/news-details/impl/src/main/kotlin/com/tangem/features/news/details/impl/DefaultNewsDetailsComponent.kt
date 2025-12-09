package com.tangem.features.news.details.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.news.details.api.NewsDetailsComponent
import com.tangem.features.news.details.impl.ui.NewsDetailsContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultNewsDetailsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: NewsDetailsComponent.Params,
) : NewsDetailsComponent, AppComponentContext by context {

    private val model: NewsDetailsModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val uiState by model.uiState.collectAsState()
        NewsDetailsContent(
            state = uiState,
            onBackClick = model::onBackClick,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : NewsDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: NewsDetailsComponent.Params,
        ): DefaultNewsDetailsComponent
    }
}