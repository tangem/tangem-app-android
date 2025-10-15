package com.tangem.features.markets.portfolio.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.features.markets.portfolio.add.api.AddToPortfolioComponent
import com.tangem.features.markets.portfolio.api.MarketsPortfolioComponent
import com.tangem.features.markets.portfolio.impl.model.MarketsPortfolioModel
import com.tangem.features.markets.portfolio.impl.ui.MyPortfolio
import com.tangem.features.tokenreceive.TokenReceiveComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.serialization.builtins.serializer

@Stable
internal class DefaultMarketsPortfolioComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: MarketsPortfolioComponent.Params,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
    private val addToPortfolioComponentFactory: AddToPortfolioComponent.Factory,
    private val accountsFeatureToggles: AccountsFeatureToggles,
) : AppComponentContext by context, MarketsPortfolioComponent {

    private val model: MarketsPortfolioModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = TokenReceiveConfig.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )
    private val addToPortfolioSlot: Value<ChildSlot<Unit, ComposableBottomSheetComponent>>?

    init {
        if (accountsFeatureToggles.isFeatureEnabled) {
            addToPortfolioSlot = childSlot(
                key = "addToPortfolioSlot",
                source = model.addToPortfolioNavigation,
                serializer = Unit.serializer(),
                handleBackButton = false,
                childFactory = { _, componentContext ->
                    addToPortfolioComponentFactory.create(
                        context = childByContext(componentContext),
                        params = AddToPortfolioComponent.Params(
                            addToPortfolioManager = model.newAddToPortfolioManager!!,
                            callback = model.addToPortfolioCallback,
                        ),
                    )
                },
            )
        } else {
            addToPortfolioSlot = null
        }
    }

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
        val addToPortfolioSlot = addToPortfolioSlot?.subscribeAsState()

        MyPortfolio(modifier = modifier, state = state)
        bottomSheet.child?.instance?.BottomSheet()
        addToPortfolioSlot?.value?.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: TokenReceiveConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = tokenReceiveComponentFactory.create(
        context = childByContext(componentContext),
        params = TokenReceiveComponent.Params(
            config = config,
            onDismiss = model.bottomSheetNavigation::dismiss,
        ),
    )

    @AssistedFactory
    interface Factory : MarketsPortfolioComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: MarketsPortfolioComponent.Params,
        ): DefaultMarketsPortfolioComponent
    }
}