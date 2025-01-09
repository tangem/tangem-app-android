package com.tangem.features.onramp.confirmresidency

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.onramp.confirmresidency.entity.ConfirmResidencyBottomSheetConfig
import com.tangem.features.onramp.confirmresidency.model.ConfirmResidencyModel
import com.tangem.features.onramp.confirmresidency.ui.ConfirmResidencyBottomSheet
import com.tangem.features.onramp.confirmresidency.ui.ConfirmResidencyBottomSheetContent
import com.tangem.features.onramp.selectcountry.SelectCountryComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultConfirmResidencyComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: ConfirmResidencyComponent.Params,
    private val selectCountryComponentFactory: SelectCountryComponent.Factory,
) : ConfirmResidencyComponent, AppComponentContext by context {

    private val model: ConfirmResidencyModel = getOrCreateModel(params)
    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    override fun dismiss() {
        router.pop()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.state.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        val bottomSheetConfig = remember(key1 = this) {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = TangemBottomSheetConfigContent.Empty,
            )
        }
        ConfirmResidencyBottomSheet(
            config = bottomSheetConfig,
            content = { modifier ->
                ConfirmResidencyBottomSheetContent(model = state, modifier = modifier)
            },
        )

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: ConfirmResidencyBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        is ConfirmResidencyBottomSheetConfig.SelectCountry -> selectCountryComponentFactory.create(
            context = childByContext(componentContext),
            params = SelectCountryComponent.Params(
                cryptoCurrency = params.cryptoCurrency,
                onDismiss = {
                    // Dismiss country select sheet
                    model.bottomSheetNavigation.dismiss()

                    // Dismiss confirm residency sheet
                    config.onDismiss()
                },
            ),
        )
    }

    @AssistedFactory
    interface Factory : ConfirmResidencyComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: ConfirmResidencyComponent.Params,
        ): DefaultConfirmResidencyComponent
    }
}