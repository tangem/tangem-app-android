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
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.foryou.TokenSummaryComponent
import com.tangem.features.foryou.impl.tokensummary.entity.InfoBottomSheetContent
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSummaryBottomSheetConfig
import com.tangem.features.foryou.impl.tokensummary.model.TokenSummaryModel
import com.tangem.features.foryou.impl.tokensummary.ui.TokenSummaryContent
import com.tangem.features.foryou.impl.tokensummary.ui.components.InfoBottomSheet
import com.tangem.features.foryou.impl.tokensummary.ui.components.TokenSummaryTopNavigation
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTokenSummaryComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: TokenSummaryComponent.Params,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) : TokenSummaryComponent, AppComponentContext by context {

    private val model: TokenSummaryModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = TokenSummaryBottomSheetConfig.serializer(),
        handleBackButton = false,
        childFactory = { config, componentContext ->
            when (config) {
                TokenSummaryBottomSheetConfig.PortfolioSelector -> portfolioSelectorChild(componentContext)
                is TokenSummaryBottomSheetConfig.Info -> infoChild(config)
            }
        },
    )

    private fun portfolioSelectorChild(componentContext: ComponentContext): ComposableBottomSheetComponent =
        portfolioSelectorComponentFactory.create(
            context = childByContext(componentContext),
            params = PortfolioSelectorComponent.Params(
                portfolioFetcher = model.portfolioFetcher,
                controller = model.portfolioSelectorController,
                bsCallback = model.portfolioSelectorCallback,
            ),
        )

    private fun infoChild(config: TokenSummaryBottomSheetConfig.Info): ComposableBottomSheetComponent =
        object : ComposableBottomSheetComponent {
            override fun dismiss() = model.bottomSheetNavigation.dismiss()

            @Composable
            override fun BottomSheet() {
                InfoBottomSheet(
                    infoBottomSheetContent = InfoBottomSheetContent(
                        title = stringReference(config.indicatorType.title),
                        body = stringReference("helps to estimate the token's momentum and market sentiment."),
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