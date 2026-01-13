package com.tangem.features.feed.components.market.details.portfolio.add.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.addtoken.AddTokenContent
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.feed.components.market.details.portfolio.add.SelectedNetwork
import com.tangem.features.feed.components.market.details.portfolio.add.SelectedPortfolio
import com.tangem.features.feed.components.market.details.portfolio.add.impl.model.AddTokenModel
import com.tangem.features.feed.components.market.details.portfolio.impl.analytics.PortfolioAnalyticsEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow

internal class AddTokenComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
) : AppComponentContext by context, ComposableContentComponent {

    private val model: AddTokenModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state = model.uiState.collectAsStateWithLifecycle()
        val um = state.value ?: return
        AddTokenContent(
            modifier = modifier,
            state = um,
        )
    }

    data class Params(
        val eventBuilder: PortfolioAnalyticsEvent.EventBuilder,
        val selectedPortfolio: Flow<SelectedPortfolio>,
        val selectedNetwork: Flow<SelectedNetwork>,
        val callbacks: Callbacks,
    )

    interface Callbacks {
        fun onChangeNetworkClick()
        fun onChangePortfolioClick()
        fun onTokenAdded(status: CryptoCurrencyStatus)
    }

    @AssistedFactory
    interface Factory : ComponentFactory<Params, AddTokenComponent> {
        override fun create(context: AppComponentContext, params: Params): AddTokenComponent
    }
}