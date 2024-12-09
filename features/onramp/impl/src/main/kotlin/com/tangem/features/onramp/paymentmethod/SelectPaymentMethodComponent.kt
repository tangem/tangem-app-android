package com.tangem.features.onramp.paymentmethod

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.onramp.paymentmethod.entity.PaymentMethodUM

internal interface SelectPaymentMethodComponent : ComposableBottomSheetComponent {

    data class Params(
        val selectedMethodId: String,
        val paymentMethods: List<PaymentMethodUM>,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, SelectPaymentMethodComponent>
}