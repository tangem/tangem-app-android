package com.tangem.features.tangempay.orderCard.impl.ui.state

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface TangemPayOrderCardDataScreenUM {

    val onBackClick: () -> Unit
    val onCloseClick: () -> Unit

    @Immutable
    data class Loading(
        override val onBackClick: () -> Unit,
        override val onCloseClick: () -> Unit,
    ) : TangemPayOrderCardDataScreenUM

    @Immutable
    data class Error(
        override val onBackClick: () -> Unit,
        override val onCloseClick: () -> Unit,
        val onRetry: () -> Unit,
    ) : TangemPayOrderCardDataScreenUM

    @Immutable
    data class Form(
        override val onBackClick: () -> Unit,
        override val onCloseClick: () -> Unit,
        val country: String,
        val email: String,
        val phoneMask: String,
        val embossName: FieldUM,
        val firstName: FieldUM,
        val lastName: FieldUM,
        val region: FieldUM,
        val city: FieldUM,
        val addressLine1: FieldUM,
        val addressLine2: FieldUM,
        val postalCode: FieldUM,
        val phone: FieldUM,
        val isOrderEnabled: Boolean,
        val onOrderClick: () -> Unit,
    ) : TangemPayOrderCardDataScreenUM

    @Immutable
    data class FieldUM(
        val value: String,
        val error: OrderFieldError?,
        val isRequired: Boolean,
        val onValueChange: (String) -> Unit,
        val onFocusChange: (Boolean) -> Unit,
    )
}

internal enum class OrderFieldError { Required, Invalid }