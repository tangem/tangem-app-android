package com.tangem.features.onramp.paymentmethod

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.features.onramp.paymentmethod.entity.PaymentMethodsBottomSheetConfig
import com.tangem.features.onramp.paymentmethod.ui.SelectPaymentMethodBottomSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.toImmutableList

internal class DefaultSelectPaymentMethodComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: SelectPaymentMethodComponent.Params,
) : SelectPaymentMethodComponent, AppComponentContext by context {

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val bottomSheetConfig = remember(key1 = this) {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = PaymentMethodsBottomSheetConfig(
                    selectedMethodId = params.selectedMethodId,
                    paymentMethods = params.paymentMethods.toImmutableList(),
                ),
            )
        }
        SelectPaymentMethodBottomSheet(bottomSheetConfig)
    }

    @AssistedFactory
    interface Factory : SelectPaymentMethodComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SelectPaymentMethodComponent.Params,
        ): DefaultSelectPaymentMethodComponent
    }
}