package com.tangem.features.moneyaccount.onboarding.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.moneyaccount.onboarding.model.MoneyAccountOnboardingModel
import com.tangem.features.moneyaccount.onboarding.ui.MoneyAccountOnboardingNavigation
import com.tangem.features.moneyaccount.onboarding.ui.MoneyAccountOnboardingScreen
import com.tangem.features.tangempay.eligibility.TangemPayWalletSelectorComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultMoneyAccountOnboardingComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: MoneyAccountOnboardingComponent.Params,
    private val walletSelectorComponentFactory: TangemPayWalletSelectorComponent.Factory,
) : MoneyAccountOnboardingComponent, AppComponentContext by appComponentContext {

    private val model: MoneyAccountOnboardingModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = MoneyAccountOnboardingNavigation.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        MoneyAccountOnboardingScreen(
            modifier = modifier,
            state = state,
        )
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        navigation: MoneyAccountOnboardingNavigation,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        val context = childByContext(componentContext)
        return when (navigation) {
            is MoneyAccountOnboardingNavigation.WalletSelector -> walletSelectorComponentFactory.create(
                context = context,
                params = TangemPayWalletSelectorComponent.Params(listener = model, walletsIds = navigation.walletsIds),
            )
        }
    }

    @AssistedFactory
    interface Factory : MoneyAccountOnboardingComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: MoneyAccountOnboardingComponent.Params,
        ): DefaultMoneyAccountOnboardingComponent
    }
}