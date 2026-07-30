package com.tangem.features.tangempay.orderCard.impl.ui.state

import androidx.compose.runtime.Immutable

@Immutable
internal data class TangemPayOrderCardTypeUM(
    val isLoading: Boolean,
    val isError: Boolean,
    val availableTypes: List<OrderCardType>,
    val cardImageUrl: String?,
    val virtual: Virtual,
    val plastic: Plastic?,
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
    data class Plastic(
        val country: String,
        val deliveryFee: String,
        val deliveryEtaMaxBusinessDays: Int,
        val feeState: FeeState,
    )

    enum class FeeState { Default, FreeDelivery, InsufficientFunds }

    companion object {
        @Suppress("MagicNumber")
        fun stub(
            isLoading: Boolean = false,
            isError: Boolean = false,
            isPlasticAvailable: Boolean = true,
            cardImageUrl: String? = null,
            issueFee: String = "$5",
            country: String = "Afghanistan",
            deliveryFee: String = "$10",
            deliveryEtaMaxBusinessDays: Int = 20,
            feeState: FeeState = FeeState.Default,
        ) = TangemPayOrderCardTypeUM(
            isLoading = isLoading,
            isError = isError,
            availableTypes = availableTypesOf(isPlasticAvailable),
            cardImageUrl = cardImageUrl,
            virtual = Virtual(issueFee = issueFee),
            plastic = if (isPlasticAvailable) {
                Plastic(
                    country = country,
                    deliveryFee = deliveryFee,
                    deliveryEtaMaxBusinessDays = deliveryEtaMaxBusinessDays,
                    feeState = feeState,
                )
            } else {
                null
            },
            onBackClick = {},
            onRetry = {},
            onSelectVirtual = {},
            onSelectPlastic = {},
        )
    }
}

internal enum class OrderCardType { Virtual, Plastic }

internal fun availableTypesOf(isPlasticAvailable: Boolean): List<OrderCardType> = if (isPlasticAvailable) {
    listOf(OrderCardType.Virtual, OrderCardType.Plastic)
} else {
    listOf(OrderCardType.Virtual)
}