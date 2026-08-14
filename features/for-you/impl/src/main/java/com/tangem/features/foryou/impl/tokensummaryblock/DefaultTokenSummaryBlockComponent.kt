package com.tangem.features.foryou.impl.tokensummaryblock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.foryou.TokenSummaryBlockComponent
import com.tangem.features.foryou.impl.tokensummaryblock.model.TokenSummaryBlockModel
import com.tangem.features.foryou.impl.tokensummaryblock.ui.TokenSummaryBlock
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTokenSummaryBlockComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: TokenSummaryBlockComponent.Params,
) : TokenSummaryBlockComponent, AppComponentContext by context {

    private val model: TokenSummaryBlockModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        TokenSummaryBlock(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : TokenSummaryBlockComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TokenSummaryBlockComponent.Params,
        ): DefaultTokenSummaryBlockComponent
    }
}