package com.tangem.feature.referral

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
import com.tangem.feature.referral.api.ReferralComponent
import com.tangem.feature.referral.model.ReferralModel
import com.tangem.feature.referral.ui.ReferralScreen
import com.tangem.features.account.PortfolioSelectorComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.serialization.builtins.serializer

class DefaultReferralComponent @AssistedInject constructor(
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: ReferralComponent.Params,
) : ReferralComponent, AppComponentContext by appComponentContext {

    private val model: ReferralModel = getOrCreateModel(params)
    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = Unit.serializer(),
        handleBackButton = false,
        childFactory = { configuration, context -> bottomSheetChild(context) },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        ReferralScreen(stateHolder = model.uiState)
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(componentContext: ComponentContext): ComposableBottomSheetComponent =
        portfolioSelectorComponentFactory.create(
            context = childByContext(componentContext),
            params = PortfolioSelectorComponent.Params(
                portfolioFetcher = model.portfolioFetcher,
                controller = model.portfolioSelectorController,
                bsCallback = model.portfolioSelectorCallback,
            ),
        )

    @AssistedFactory
    interface Factory : ReferralComponent.Factory {
        override fun create(context: AppComponentContext, params: ReferralComponent.Params): DefaultReferralComponent
    }
}