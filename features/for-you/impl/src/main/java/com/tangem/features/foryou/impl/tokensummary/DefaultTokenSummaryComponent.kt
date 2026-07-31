package com.tangem.features.foryou.impl.tokensummary

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
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
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioComponent
import com.tangem.features.commonfeatures.api.managefunds.ManageFundsComponent
import com.tangem.features.foryou.TokenSummaryComponent
import com.tangem.features.foryou.impl.tokensummary.entity.InfoBottomSheetContent
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSummaryBottomSheetConfig
import com.tangem.features.foryou.impl.tokensummary.model.TokenSummaryModel
import com.tangem.features.foryou.impl.tokensummary.swapchooser.SwapTokenChooserComponent
import com.tangem.features.foryou.impl.tokensummary.ui.TokenSummaryContent
import com.tangem.features.foryou.impl.tokensummary.ui.components.InfoBottomSheet
import com.tangem.features.foryou.impl.tokensummary.ui.components.TokenSummaryTopNavigation
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTokenSummaryComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: TokenSummaryComponent.Params,
    private val swapTokenChooserComponentFactory: SwapTokenChooserComponent.Factory,
    private val manageFundsComponentFactory: ManageFundsComponent.Factory,
    private val addToPortfolioComponentFactory: AddToPortfolioComponent.Factory,
) : TokenSummaryComponent, AppComponentContext by context {

    private val model: TokenSummaryModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        // Sheets are not restored after process death: the chooser renders holdings the model resolves anew, and a
        // sheet brought back before they arrive would sit there active and empty. The summary itself is enough.
        serializer = null,
        handleBackButton = false,
        childFactory = { config, componentContext ->
            when (config) {
                is TokenSummaryBottomSheetConfig.SwapChooser -> swapChooserChild(componentContext)
                is TokenSummaryBottomSheetConfig.ManageFunds -> manageFundsChild(config, componentContext)
                is TokenSummaryBottomSheetConfig.AddToPortfolio -> addToPortfolioChild(componentContext)
                is TokenSummaryBottomSheetConfig.Info -> infoChild(config)
            }
        },
    )

    private fun swapChooserChild(componentContext: ComponentContext): ComposableBottomSheetComponent =
        swapTokenChooserComponentFactory.create(
            context = childByContext(componentContext),
            params = SwapTokenChooserComponent.Params(
                holdings = model.swapHoldings,
                callbacks = model.swapChooserCallbacks,
            ),
        )

    private fun addToPortfolioChild(componentContext: ComponentContext): ComposableBottomSheetComponent =
        addToPortfolioComponentFactory.create(
            context = childByContext(componentContext),
            params = AddToPortfolioComponent.Params(
                addToPortfolioManager = checkNotNull(model.addToPortfolioManager) {
                    "addToPortfolioManager must be set before activating AddToPortfolio slot"
                },
            ),
        )

    private fun manageFundsChild(
        config: TokenSummaryBottomSheetConfig.ManageFunds,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = manageFundsComponentFactory.create(
        context = childByContext(componentContext),
        params = ManageFundsComponent.Params(
            launchMode = ManageFundsComponent.LaunchMode.FilteredByRawId(config.rawCurrencyId),
            onDismiss = { model.bottomSheetNavigation.dismiss() },
        ),
    )

    private fun infoChild(config: TokenSummaryBottomSheetConfig.Info): ComposableBottomSheetComponent =
        object : ComposableBottomSheetComponent {
            override fun dismiss() = model.bottomSheetNavigation.dismiss()

            @Composable
            override fun BottomSheet() {
                InfoBottomSheet(
                    infoBottomSheetContent = InfoBottomSheetContent(
                        title = stringReference(config.title),
                        body = config.indicatorType.description,
                    ),
                    onDismiss = ::dismiss,
                )
            }
        }

    @Composable
    override fun Title(bottomSheetState: State<BottomSheetState>) {
        val uiState by model.uiState.collectAsStateWithLifecycle()

        TokenSummaryTopNavigation(
            header = uiState.header,
            onCloseClick = uiState.onCloseClick,
        )
    }

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        modifier: Modifier,
    ) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheetSlot by bottomSheetSlot.subscribeAsState()

        TokenSummaryContent(
            tokenSummary = state,
            contentPadding = contentPadding,
            modifier = modifier,
        )

        bottomSheetSlot.child?.instance?.BottomSheet()
    }

    @AssistedFactory
    interface Factory : TokenSummaryComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TokenSummaryComponent.Params,
        ): DefaultTokenSummaryComponent
    }
}