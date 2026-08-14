package com.tangem.features.jointaccount.join.invitepreview

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
import com.tangem.features.jointaccount.join.invitepreview.model.JointAccountInvitePreviewModel
import com.tangem.features.jointaccount.join.invitepreview.ui.JointAccountInvitePreviewScreen
import com.tangem.features.jointaccount.join.model.JointAccountJoinChildParams
import kotlinx.serialization.builtins.serializer

internal class JointAccountInvitePreviewComponent(
    appComponentContext: AppComponentContext,
    params: JointAccountJoinChildParams,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: JointAccountInvitePreviewModel = getOrCreateModel(params = params)

    private val portfolioSelectorSlot = childSlot(
        source = model.portfolioSelectorNavigation,
        serializer = Unit.serializer(),
        key = "jointAccountJoinPortfolioSelectorSlot",
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

        JointAccountInvitePreviewScreen(state = state, modifier = modifier)

        selectorSlot.child?.instance?.BottomSheet()
    }
}