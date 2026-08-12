package com.tangem.features.polymarket.impl.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.polymarket.api.PolymarketComponent
import com.tangem.features.polymarket.impl.entry.model.PolymarketEntryModel
import com.tangem.features.polymarket.impl.entry.ui.PolymarketEntryScreen

/**
 * Entry point of the feature stack — settles the wallet and its deposit network before
 * `PolymarketOnboardingComponent` runs.
 *

 * factory — its model is resolved from the model map by [getOrCreateModel].
 *
 * @param params feature params the model reads the caller-supplied wallet from; the model reads them back out
 *  of its params container, so they must be handed over here.
 */
internal class PolymarketEntryComponent(
    appComponentContext: AppComponentContext,
    params: PolymarketComponent.Params,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
    private val addToPortfolioComponentFactory: AddToPortfolioComponent.Factory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketEntryModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    private fun bottomSheetChild(
        config: PolymarketEntryBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        PolymarketEntryBottomSheetConfig.WalletSelector -> portfolioSelectorChild(componentContext)
        PolymarketEntryBottomSheetConfig.AddDepositNetwork -> addToPortfolioChild(componentContext)
    }

    private fun portfolioSelectorChild(componentContext: ComponentContext): ComposableBottomSheetComponent =
        portfolioSelectorComponentFactory.create(
            context = childByContext(componentContext),
            params = PortfolioSelectorComponent.Params(
                portfolioFetcher = model.portfolioFetcher,
                controller = model.portfolioSelectorController,
                bsCallback = model.portfolioSelectorCallback,
                settings = PortfolioSelectorComponent.Settings(isWalletSelectionOnly = true),
            ),
        )

    private fun addToPortfolioChild(componentContext: ComponentContext): ComposableBottomSheetComponent =
        addToPortfolioComponentFactory.create(
            context = childByContext(componentContext),
            params = AddToPortfolioComponent.Params(
                addToPortfolioManager = checkNotNull(model.addToPortfolioManager) {
                    "addToPortfolioManager must be set before activating the AddToPortfolio slot"
                },
            ),
        )

    @Composable
    override fun Content(modifier: Modifier) {
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        PolymarketEntryScreen(modifier = modifier)
        bottomSheet.child?.instance?.BottomSheet()
    }
}