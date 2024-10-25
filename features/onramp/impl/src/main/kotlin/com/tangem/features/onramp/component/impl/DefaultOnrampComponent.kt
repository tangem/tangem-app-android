package com.tangem.features.onramp.component.impl

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.onramp.component.OnrampComponent
import com.tangem.features.onramp.component.ConfirmResidencyComponent
import com.tangem.features.onramp.entity.OnrampBottomSheetConfig
import com.tangem.features.onramp.model.OnrampModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnrampComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: OnrampComponent.Params,
    private val confirmResidencyComponentFactory: ConfirmResidencyComponent.Factory,
) : OnrampComponent, AppComponentContext by context {

    private val model: OnrampModel = getOrCreateModel(params)
    private val confirmResidencyBottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val bottomSheet by confirmResidencyBottomSheetSlot.subscribeAsState()
        BackHandler(onBack = router::pop)

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: OnrampBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        OnrampBottomSheetConfig.ConfirmResidency -> confirmResidencyComponentFactory.create(
            context = childByContext(componentContext),
            params = ConfirmResidencyComponent.Params(
                countryName = "United States",
                isOnrampSupported = true,
                countryFlagUrl = "https://hatscripts.github.io/circle-flags/flags/us.svg",
                onDismiss = model.bottomSheetNavigation::dismiss,
            ),
        )
    }

    @AssistedFactory
    interface Factory : OnrampComponent.Factory {
        override fun create(context: AppComponentContext, params: OnrampComponent.Params): DefaultOnrampComponent
    }
}
