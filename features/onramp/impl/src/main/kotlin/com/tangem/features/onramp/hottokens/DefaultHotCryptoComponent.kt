package com.tangem.features.onramp.hottokens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.onramp.hottokens.model.HotCryptoModel
import com.tangem.features.onramp.hottokens.portfolio.OnrampAddToPortfolioComponent
import com.tangem.features.onramp.hottokens.portfolio.entity.OnrampAddToPortfolioBSConfig
import com.tangem.features.onramp.hottokens.ui.HotCrypto
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultHotCryptoComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: HotCryptoComponent.Params,
    private val onrampAddToPortfolioComponentFactory: OnrampAddToPortfolioComponent.Factory,
) : HotCryptoComponent, AppComponentContext by context {

    private val model: HotCryptoModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state = model.state.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        HotCrypto(state, modifier)

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: OnrampAddToPortfolioBSConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        return when (config) {
            is OnrampAddToPortfolioBSConfig.AddToPortfolio -> {
                onrampAddToPortfolioComponentFactory.create(
                    context = childByContext(componentContext),
                    params = OnrampAddToPortfolioComponent.Params(
                        userWalletId = params.userWalletId,
                        cryptoCurrency = config.cryptoCurrency,
                        currencyIconState = config.currencyIconState,
                        onSuccessAdding = config.onSuccessAdding,
                        onDismiss = model.bottomSheetNavigation::dismiss,
                    ),
                )
            }
        }
    }

    @AssistedFactory
    interface Factory : HotCryptoComponent.Factory {
        override fun create(context: AppComponentContext, params: HotCryptoComponent.Params): DefaultHotCryptoComponent
    }
}