package com.tangem.features.onramp.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.onramp.confirmresidency.ConfirmResidencyComponent
import com.tangem.features.onramp.main.entity.OnrampMainBottomSheetConfig
import com.tangem.features.onramp.main.model.OnrampMainComponentModel
import com.tangem.features.onramp.main.ui.OnrampMainComponentContent
import com.tangem.features.onramp.providers.SelectProviderComponent
import com.tangem.features.onramp.selectcurrency.SelectCurrencyComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnrampMainComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: OnrampMainComponent.Params,
    private val confirmResidencyComponentFactory: ConfirmResidencyComponent.Factory,
    private val selectCurrencyComponentFactory: SelectCurrencyComponent.Factory,
    private val selectProviderComponentFactory: SelectProviderComponent.Factory,
) : OnrampMainComponent, AppComponentContext by appComponentContext {

    private val model: OnrampMainComponentModel = getOrCreateModel(params)
    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsState()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        OnrampMainComponentContent(modifier = modifier, state = state)
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: OnrampMainBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        is OnrampMainBottomSheetConfig.ConfirmResidency -> confirmResidencyComponentFactory.create(
            context = childByContext(componentContext),
            params = ConfirmResidencyComponent.Params(
                cryptoCurrency = params.cryptoCurrency,
                country = config.country,
                onDismiss = {
                    model.bottomSheetNavigation.dismiss()
                    model.handleOnrampAvailable()
                },
            ),
        )
        is OnrampMainBottomSheetConfig.CurrenciesList -> selectCurrencyComponentFactory.create(
            context = childByContext(componentContext),
            params = SelectCurrencyComponent.Params(
                cryptoCurrency = params.cryptoCurrency,
                onDismiss = model.bottomSheetNavigation::dismiss,
            ),
        )
        is OnrampMainBottomSheetConfig.ProvidersList -> selectProviderComponentFactory.create(
            context = childByContext(componentContext),
            params = SelectProviderComponent.Params(
                onProviderClick = model::onProviderSelected,
                onDismiss = model.bottomSheetNavigation::dismiss,
                selectedProviderId = config.selectedProviderId,
                selectedPaymentMethod = config.selectedPaymentMethod,
                cryptoCurrency = params.cryptoCurrency,
            ),
        )
    }

    @AssistedFactory
    interface Factory : OnrampMainComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnrampMainComponent.Params,
        ): DefaultOnrampMainComponent
    }
}