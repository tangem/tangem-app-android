package com.tangem.features.feed.components.market.details.portfolioblock

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
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.feed.components.market.details.portfolioblock.model.PortfolioBlockModel
import com.tangem.features.feed.components.market.details.portfolioblock.model.PortfolioBlockRoute
import com.tangem.features.feed.components.market.details.portfolioblock.ui.PortfolioBlock
import com.tangem.features.feed.components.portfolio.PortfolioComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.serialization.Serializable

@Stable
internal class PortfolioBlockComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val portfolioComponentFactory: PortfolioComponent.Factory = object : PortfolioComponent.Factory {
        override fun create(context: AppComponentContext, params: PortfolioComponent.Params): PortfolioComponent {
            TODO("STUB. Will be implemented")
        }
    }

    @Serializable
    data class Params(
        val token: TokenMarketParams,
    )

    private val model: PortfolioBlockModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = PortfolioBlockRoute.serializer(),
        handleBackButton = true,
        childFactory = ::createBottomSheetChild,
    )

    fun setTokenNetworks(networks: List<TokenMarketInfo.Network>) {
        model.setTokenNetworks(networks)
    }

    fun setNoNetworksAvailable() {
        model.setNoNetworksAvailable()
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        PortfolioBlock(modifier = modifier, state = state)
        bottomSheet.child?.instance?.BottomSheet()
    }

    @Suppress("UnusedParameter")
    private fun createBottomSheetChild(
        config: PortfolioBlockRoute,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        val currencyId = model.cryptoCurrencyIdState.value ?: return ComposableBottomSheetComponent.EMPTY
        val portfolioComponent = portfolioComponentFactory.create(
            context = childByContext(componentContext),
            params = PortfolioComponent.Params(id = currencyId),
        )
        return PortfolioBottomSheetWrapper(
            portfolioComponent = portfolioComponent,
            onDismiss = { model.bottomSheetNavigation.dismiss() },
        )
    }

    private class PortfolioBottomSheetWrapper(
        private val portfolioComponent: PortfolioComponent,
        private val onDismiss: () -> Unit,
    ) : ComposableBottomSheetComponent {

        override fun dismiss() = onDismiss()

        @Composable
        override fun BottomSheet() {
            TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
                config = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = ::dismiss,
                    content = TangemBottomSheetConfigContent.Empty,
                ),
            ) {
                portfolioComponent.Content(modifier = Modifier)
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): PortfolioBlockComponent
    }
}