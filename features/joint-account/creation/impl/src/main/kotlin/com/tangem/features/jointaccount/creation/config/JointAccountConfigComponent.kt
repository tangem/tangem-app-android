package com.tangem.features.jointaccount.creation.config

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.jointaccount.creation.config.model.JointAccountConfigModel
import com.tangem.features.jointaccount.creation.config.ui.JointAccountConfigScreen
import com.tangem.features.jointaccount.creation.model.JointAccountCreationChildParams
import kotlinx.serialization.builtins.serializer

internal class JointAccountConfigComponent(
    appComponentContext: AppComponentContext,
    params: JointAccountCreationChildParams,
    private val onCloseClick: () -> Unit,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: JointAccountConfigModel = getOrCreateModel(params = params)

    private val portfolioSelectorSlot = childSlot(
        source = model.portfolioSelectorNavigation,
        serializer = Unit.serializer(),
        key = "jointAccountConfigPortfolioSelectorSlot",
        handleBackButton = false,
        childFactory = { _, componentContext ->
            portfolioSelectorComponentFactory.create(
                context = childByContext(componentContext),
                params = PortfolioSelectorComponent.Params(
                    portfolioFetcher = model.portfolioFetcher,
                    controller = model.portfolioSelectorController,
                    bsCallback = model.portfolioSelectorCallback,
                    settings = PortfolioSelectorComponent.Settings(isWalletSelectionOnly = true),
                ),
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val selectorSlot by portfolioSelectorSlot.subscribeAsState()

        JointAccountConfigScreen(state = state, onCloseClick = onCloseClick, modifier = modifier)

        selectorSlot.child?.instance?.BottomSheet()
    }
}