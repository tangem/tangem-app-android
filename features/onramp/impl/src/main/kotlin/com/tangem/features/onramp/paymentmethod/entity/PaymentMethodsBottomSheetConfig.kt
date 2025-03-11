package com.tangem.features.onramp.paymentmethod.entity

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.collections.immutable.ImmutableList

internal data class PaymentMethodsBottomSheetConfig(
    val selectedMethodId: String,
    val paymentMethods: ImmutableList<PaymentMethodUM>,
) : TangemBottomSheetConfigContent