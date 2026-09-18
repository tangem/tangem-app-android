package com.tangem.features.tangempay.orderCard.impl.ui.state

import androidx.compose.runtime.Immutable

@Immutable
internal data class TangemPayOrderCardTypeUM(
    val isLoading: Boolean,
    val isError: Boolean,
    val availableTypes: List<OrderCardType>,
    val cardImageUrl: String?,
    val isBasicPlan: Boolean,
    val virtual: Virtual,
    val plastic: Plastic,
    val onBackClick: () -> Unit,
    val onRetry: () -> Unit,
    val onSelectVirtual: () -> Unit,
    val onSelectPlastic: () -> Unit,
    val onTypeClick: (OrderCardType) -> Unit,
    val onTypeSwipe: (OrderCardType) -> Unit,
) {

    @Immutable
    data class Virtual(
        val issueFee: String,
        val offerImageUrl: String? = null,
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
            val offerImageUrl: String? = null,
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

internal fun TangemPayOrderCardTypeUM.imageUrlFor(type: OrderCardType): String? = when (type) {
    OrderCardType.Virtual -> virtual.offerImageUrl
    OrderCardType.Plastic -> when (val plastic = plastic) {
        is TangemPayOrderCardTypeUM.Plastic.Available -> plastic.offerImageUrl
        is TangemPayOrderCardTypeUM.Plastic.Unavailable -> null
    }
} ?: cardImageUrl.takeIf { !isLoading }

internal fun availableTypesOf(isPlasticEnabled: Boolean, isVirtualAvailable: Boolean = true): List<OrderCardType> =
    buildList {
        if (isVirtualAvailable || !isPlasticEnabled) add(OrderCardType.Virtual)
        if (isPlasticEnabled) add(OrderCardType.Plastic)
    }