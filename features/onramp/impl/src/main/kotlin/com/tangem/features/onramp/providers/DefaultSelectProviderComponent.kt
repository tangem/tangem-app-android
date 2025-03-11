package com.tangem.features.onramp.providers

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.onramp.paymentmethod.SelectPaymentMethodComponent
import com.tangem.features.onramp.providers.entity.ProviderListBottomSheetConfig
import com.tangem.features.onramp.providers.model.SelectProviderModel
import com.tangem.features.onramp.providers.ui.SelectProviderBottomSheet
import com.tangem.features.onramp.providers.ui.SelectProviderBottomSheetContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultSelectProviderComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: SelectProviderComponent.Params,
    private val selectPaymentMethodComponentFactory: SelectPaymentMethodComponent.Factory,
) : SelectProviderComponent, AppComponentContext by context {

    private val model: SelectProviderModel = getOrCreateModel(params)
    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    override fun dismiss() {
        params.onDismiss()
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
        SelectProviderBottomSheet(
            config = bottomSheetConfig,
            content = {
                SelectProviderBottomSheetContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = TangemTheme.dimens.spacing16),
                    state = state,
                )
            },
        )
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: ProviderListBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        is ProviderListBottomSheetConfig.PaymentMethods -> selectPaymentMethodComponentFactory.create(
            context = childByContext(componentContext),
            params = SelectPaymentMethodComponent.Params(
                selectedMethodId = config.selectedMethodId,
                paymentMethods = config.paymentMethodsUM,
                onDismiss = model.bottomSheetNavigation::dismiss,
            ),
        )
    }

    @AssistedFactory
    interface Factory : SelectProviderComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SelectProviderComponent.Params,
        ): DefaultSelectProviderComponent
    }
}