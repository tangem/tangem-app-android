package com.tangem.features.jointaccount.creation.promo

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
import com.tangem.features.jointaccount.creation.model.JointAccountCreationChildParams
import com.tangem.features.jointaccount.creation.promo.model.JointAccountPromoModel
import com.tangem.features.jointaccount.creation.promo.ui.JointAccountPromoScreen
import kotlinx.serialization.builtins.serializer

internal class JointAccountPromoComponent(
    appComponentContext: AppComponentContext,
    params: JointAccountCreationChildParams,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: JointAccountPromoModel = getOrCreateModel(params = params)

    private val walletSelectorSlot = childSlot(
        source = model.walletPicker.navigation,
        serializer = Unit.serializer(),
        key = "jointAccountPromoWalletSelectorSlot",
        handleBackButton = false,
        childFactory = { _, componentContext ->
            portfolioSelectorComponentFactory.create(
                context = childByContext(componentContext),
                params = model.walletPicker.selectorParams,
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val walletSelector by walletSelectorSlot.subscribeAsState()

        JointAccountPromoScreen(state = state, modifier = modifier)

        walletSelector.child?.instance?.BottomSheet()
    }
}