package com.tangem.features.onramp.mainv2

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
import com.tangem.features.onramp.mainv2.entity.OnrampV2MainBottomSheetConfig
import com.tangem.features.onramp.mainv2.model.OnrampV2MainComponentModel
import com.tangem.features.onramp.mainv2.ui.OnrampNewMainScreen
import com.tangem.features.onramp.selectcurrency.SelectCurrencyComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnrampV2MainComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: OnrampV2MainComponent.Params,
    private val confirmResidencyComponentFactory: ConfirmResidencyComponent.Factory,
    private val selectCurrencyComponentFactory: SelectCurrencyComponent.Factory,
) : OnrampV2MainComponent, AppComponentContext by appComponentContext {

    private val model: OnrampV2MainComponentModel = getOrCreateModel(params)

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

        OnrampNewMainScreen(modifier = modifier, state = state)
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: OnrampV2MainBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        is OnrampV2MainBottomSheetConfig.ConfirmResidency -> confirmResidencyComponentFactory.create(
            context = childByContext(componentContext),
            params = ConfirmResidencyComponent.Params(
                userWalletId = params.userWalletId,
                cryptoCurrency = params.cryptoCurrency,
                country = config.country,
                onDismiss = { model.bottomSheetNavigation.dismiss() },
            ),
        )
        is OnrampV2MainBottomSheetConfig.CurrenciesList -> selectCurrencyComponentFactory.create(
            context = childByContext(componentContext),
            params = SelectCurrencyComponent.Params(
                userWallet = model.userWallet,
                cryptoCurrency = params.cryptoCurrency,
                onDismiss = model.bottomSheetNavigation::dismiss,
            ),
        )
    }

    @AssistedFactory
    interface Factory : OnrampV2MainComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnrampV2MainComponent.Params,
        ): DefaultOnrampV2MainComponent
    }
}