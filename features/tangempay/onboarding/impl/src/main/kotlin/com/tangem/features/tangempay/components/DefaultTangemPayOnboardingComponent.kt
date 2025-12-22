package com.tangem.features.tangempay.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.ComponentContext
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.tangempay.model.TangemPayOnboardingModel
import com.tangem.features.tangempay.ui.TangemPayOnboardingNavigation
import com.tangem.features.tangempay.ui.TandemPayOnboardingScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayOnboardingComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: TangemPayOnboardingComponent.Params,
) : TangemPayOnboardingComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayOnboardingModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = TangemPayOnboardingNavigation.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        TandemPayOnboardingScreen(modifier = modifier, state = state)
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        navigation: TangemPayOnboardingNavigation,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        val context = childByContext(componentContext)
        return when (navigation) {
            is TangemPayOnboardingNavigation.WalletSelector -> TangemPayWalletSelectorComponent(
                appComponentContext = context,
                params = TangemPayWalletSelectorComponent.Params(listener = model, walletsIds = navigation.walletsIds),
            )
        }
    }

    @AssistedFactory
    interface Factory : TangemPayOnboardingComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayOnboardingComponent.Params,
        ): DefaultTangemPayOnboardingComponent
    }
}