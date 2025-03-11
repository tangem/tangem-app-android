package com.tangem.features.onramp.swap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.onramp.component.SwapSelectTokensComponent
import com.tangem.features.onramp.swap.availablepairs.AvailableSwapPairsComponent
import com.tangem.features.onramp.swap.model.SwapSelectTokensModel
import com.tangem.features.onramp.swap.ui.SwapSelectTokens
import com.tangem.features.onramp.tokenlist.OnrampTokenListComponent
import com.tangem.features.onramp.tokenlist.entity.OnrampOperation
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultSwapSelectTokensComponent @AssistedInject constructor(
    tokenListComponentFactory: OnrampTokenListComponent.Factory,
    availableSwapPairsComponentFactory: AvailableSwapPairsComponent.Factory,
    analyticsEventHandler: AnalyticsEventHandler,
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: SwapSelectTokensComponent.Params,
) : AppComponentContext by appComponentContext, SwapSelectTokensComponent {

    private val model: SwapSelectTokensModel = getOrCreateModel(params)

    private val selectFromTokenListComponent: OnrampTokenListComponent = tokenListComponentFactory.create(
        context = child(key = "select_from_token_list"),
        params = OnrampTokenListComponent.Params(
            filterOperation = OnrampOperation.SWAP,
            userWalletId = params.userWalletId,
            onTokenClick = model::selectFromToken,
        ),
    )

    private val selectToTokenListComponent: AvailableSwapPairsComponent = availableSwapPairsComponentFactory.create(
        context = child(key = "select_to_token_list"),
        params = AvailableSwapPairsComponent.Params(
            userWalletId = params.userWalletId,
            selectedStatus = model.fromCurrencyStatus,
            onTokenClick = model::selectToToken,
        ),
    )

    init {
        analyticsEventHandler.send(event = MainScreenAnalyticsEvent.SwapScreenOpened)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state = model.state.collectAsStateWithLifecycle()

        SwapSelectTokens(
            state = state.value,
            selectFromTokenListComponent = selectFromTokenListComponent,
            selectToTokenListComponent = selectToTokenListComponent,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : SwapSelectTokensComponent.Factory {

        override fun create(
            context: AppComponentContext,
            params: SwapSelectTokensComponent.Params,
        ): DefaultSwapSelectTokensComponent
    }
}