package com.tangem.features.markets.portfolio.add.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.markets.portfolio.add.api.SelectedNetwork
import com.tangem.features.markets.portfolio.add.api.SelectedPortfolio
import com.tangem.features.markets.portfolio.add.impl.model.AddTokenModel
import com.tangem.features.markets.portfolio.add.impl.ui.AddTokenContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow

internal class AddTokenComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
) : AppComponentContext by context, ComposableContentComponent {

    private val model: AddTokenModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        AddTokenContent(
            modifier = modifier,
            state = state,
        )
    }

    data class Params(
        val marketParams: TokenMarketParams,
        val selectedPortfolio: StateFlow<SelectedPortfolio>,
        val selectedNetwork: StateFlow<SelectedNetwork>,
        val onChangeNetworkClick: () -> Unit,
        val onChangePortfolioClick: () -> Unit,
        val onTokenAdded: () -> Unit,
    )

    @AssistedFactory
    interface Factory : ComponentFactory<Params, AddTokenComponent> {
        override fun create(context: AppComponentContext, params: Params): AddTokenComponent
    }
}