package com.tangem.features.tangempay.eligibility.country

import androidx.activity.compose.BackHandler
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
import com.tangem.features.tangempay.eligibility.TangemPayEligibilityComponent
import com.tangem.features.tangempay.eligibility.TangemPayWalletSelectorComponent

internal class ResidencyListComponent(
    appComponentContext: AppComponentContext,
    params: TangemPayEligibilityComponent.Params,
    private val walletSelectorComponentFactory: TangemPayWalletSelectorComponent.Factory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: ResidencyListModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = ResidencyBottomSheetConfig.serializer(),
        handleBackButton = false,
        childFactory = { config, componentContext ->
            when (config) {
                is ResidencyBottomSheetConfig.WalletSelector -> walletSelectorComponentFactory.create(
                    context = childByContext(componentContext),
                    params = TangemPayWalletSelectorComponent.Params(
                        listener = model,
                        walletsIds = config.walletsIds,
                    ),
                )
            }
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        ResidencyListScreen(state = state, modifier = modifier)
        bottomSheet.child?.instance?.BottomSheet()

        BackHandler(onBack = state.onBackClick ?: state.onCloseClick)
    }
}