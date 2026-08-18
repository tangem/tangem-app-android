package com.tangem.features.tangempay.orderCard.impl.ui.state

import androidx.compose.runtime.Immutable

@Immutable
internal data class TangemPayOrderCardTypeUM(
    val isLoading: Boolean,
    val isError: Boolean,
    val availableTypes: List<OrderCardType>,
    val cardImageUrl: String?,
    val virtual: Virtual,
    val plastic: Plastic,
    val onBackClick: () -> Unit,
    val onRetry: () -> Unit,
    val onSelectVirtual: () -> Unit,
    val onSelectPlastic: () -> Unit,
) {

    @Immutable
    data class Virtual(
        val issueFee: String,
    )

    @Immutable
    sealed interface Plastic {

        val country: String

        @Immutable
        data class Unavailable(override val country: String) : Plastic

        @Immutable
        data class Available(
            override val country: String,
            val deliveryFee: String?,
            val deliveryEta: DeliveryEta,
            val feeState: FeeState,
        ) : Plastic
    }

    @Immutable
    data class DeliveryEta(
        val minBusinessDays: Int?,
        val maxBusinessDays: Int,
    )

    enum class FeeState { Default, FreeDelivery, InsufficientFunds }
}

internal enum class OrderCardType { Virtual, Plastic }

internal fun availableTypesOf(isPlasticEnabled: Boolean): List<OrderCardType> = if (isPlasticEnabled) {
    listOf(OrderCardType.Virtual, OrderCardType.Plastic)
} else {
    listOf(OrderCardType.Virtual)
}