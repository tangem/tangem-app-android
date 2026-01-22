package com.tangem.features.feed.components.market.details.portfolio.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioComponent
import com.tangem.features.feed.components.market.details.portfolio.api.MarketsPortfolioComponent
import com.tangem.features.feed.components.market.details.portfolio.impl.model.MarketsPortfolioModel
import com.tangem.features.feed.components.market.details.portfolio.impl.model.MarketsPortfolioRoute
import com.tangem.features.feed.components.market.details.portfolio.impl.ui.MyPortfolio
import com.tangem.features.tokenreceive.TokenReceiveComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultMarketsPortfolioComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: MarketsPortfolioComponent.Params,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
    private val addToPortfolioComponentFactory: AddToPortfolioComponent.Factory,
) : AppComponentContext by context, MarketsPortfolioComponent {

    private val model: MarketsPortfolioModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = MarketsPortfolioRoute.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    override fun setTokenNetworks(networks: List<TokenMarketInfo.Network>) {
        model.setTokenNetworks(networks)
    }

    override fun setNoNetworksAvailable() {
        model.setNoNetworksAvailable()
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        MyPortfolio(modifier = modifier, state = state)
        bottomSheet.child?.instance?.BottomSheet()
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun bottomSheetChild(
        config: MarketsPortfolioRoute,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        MarketsPortfolioRoute.AddToPortfolio -> addToPortfolioComponentFactory.create(
            context = childByContext(componentContext),
            params = AddToPortfolioComponent.Params(
                addToPortfolioManager = model.newAddToPortfolioManager!!,
                callback = model.addToPortfolioCallback,
            ),
        )
        is MarketsPortfolioRoute.TokenReceive -> tokenReceiveComponentFactory.create(
            context = childByContext(componentContext),
            params = TokenReceiveComponent.Params(
                config = config.config,
                onDismiss = model.bottomSheetNavigation::dismiss,
            ),
        )
    }

    @AssistedFactory
    interface Factory : MarketsPortfolioComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: MarketsPortfolioComponent.Params,
        ): DefaultMarketsPortfolioComponent
    }
}