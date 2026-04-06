package com.tangem.features.markets.token.block.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.features.markets.token.block.TokenMarketBlockComponent
import com.tangem.features.markets.token.block.TokenMarketBlockComponent.Params
import com.tangem.features.markets.token.block.impl.model.TokenMarketBlockModel
import com.tangem.features.markets.token.block.impl.ui.TokenMarketBlock
import com.tangem.features.markets.token.block.impl.ui.TokenMarketBlockLegacy
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultTokenMarketBlockComponent @AssistedInject constructor(
    @Assisted componentContext: AppComponentContext,
    @Assisted params: Params,
) : TokenMarketBlockComponent, AppComponentContext by componentContext {

    private val model: TokenMarketBlockModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        if (LocalRedesignEnabled.current) {
            TokenMarketBlock(
                tokenMarketBlockUM = state,
                modifier = modifier,
            )
        } else {
            TokenMarketBlockLegacy(
                state = state,
                modifier = modifier,
            )
        }
    }

    @AssistedFactory
    interface Factory : TokenMarketBlockComponent.Factory {
        override fun create(appComponentContext: AppComponentContext, params: Params): DefaultTokenMarketBlockComponent
    }
}