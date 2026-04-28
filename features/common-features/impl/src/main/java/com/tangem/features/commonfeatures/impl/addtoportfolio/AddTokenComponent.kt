package com.tangem.features.commonfeatures.impl.addtoportfolio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.addtoken.AddTokenContent
import com.tangem.common.ui.addtoken.AddTokenContentV2
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.commonfeatures.api.addtoportfolio.SelectedNetwork
import com.tangem.features.commonfeatures.api.addtoportfolio.SelectedPortfolio
import com.tangem.features.commonfeatures.impl.addtoportfolio.analytics.PortfolioAnalyticsEvent
import com.tangem.features.commonfeatures.impl.addtoportfolio.model.AddTokenModel
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
        if (LocalRedesignEnabled.current) {
            AddTokenContentV2(
                modifier = modifier,
                state = um,
            )
        } else {
            AddTokenContent(
                modifier = modifier,
                state = um,
            )
        }
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