package com.tangem.features.markets.details.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.markets.details.api.MarketsTokenDetailsComponent
import com.tangem.features.markets.details.impl.model.MarketsTokenDetailsModel
import com.tangem.features.markets.details.impl.ui.MarketsTokenDetailsContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultMarketsTokenDetailsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: MarketsTokenDetailsComponent.Params,
) : AppComponentContext by appComponentContext, MarketsTokenDetailsComponent {

    private val model: MarketsTokenDetailsModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        MarketsTokenDetailsContent(
            state = state,
            onBackClick = {},
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : MarketsTokenDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: MarketsTokenDetailsComponent.Params,
        ): DefaultMarketsTokenDetailsComponent
    }
}