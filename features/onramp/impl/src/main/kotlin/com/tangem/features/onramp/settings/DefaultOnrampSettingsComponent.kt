package com.tangem.features.onramp.settings

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.onramp.selectcountry.SelectCountryComponent
import com.tangem.features.onramp.settings.entity.OnrampSettingsConfig
import com.tangem.features.onramp.settings.model.OnrampSettingsModel
import com.tangem.features.onramp.settings.ui.OnrampSettingsContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnrampSettingsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: OnrampSettingsComponent.Params,
    private val selectCountryComponentFactory: SelectCountryComponent.Factory,
) : OnrampSettingsComponent, AppComponentContext by appComponentContext {

    private val model: OnrampSettingsModel = getOrCreateModel(params)
    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        BackHandler(onBack = params.onBack)
        OnrampSettingsContent(modifier = modifier, state = state)

        val bottomSheet by bottomSheetSlot.subscribeAsState()
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: OnrampSettingsConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        OnrampSettingsConfig.SelectCountry -> selectCountryComponentFactory.create(
            context = childByContext(componentContext),
            params = SelectCountryComponent.Params(
                params.cryptoCurrency,
                model.bottomSheetNavigation::dismiss,
            ),
        )
    }

    @AssistedFactory
    interface Factory : OnrampSettingsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnrampSettingsComponent.Params,
        ): DefaultOnrampSettingsComponent
    }
}