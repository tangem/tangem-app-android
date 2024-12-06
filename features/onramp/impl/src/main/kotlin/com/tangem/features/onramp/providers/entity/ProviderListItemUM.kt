package com.tangem.features.onramp.providers.entity

import com.tangem.core.ui.extensions.TextReference

internal sealed interface ProviderListItemUM {

    val providerId: String

    data class Available(
        override val providerId: String,
        val imageUrl: String,
        val name: String,
        val rate: String,
        val isBestRate: Boolean,
        val diffRate: TextReference,
        val isSelected: Boolean,
        val onClick: () -> Unit,
    ) : ProviderListItemUM

    data class AvailableWithError(
        override val providerId: String,
        val imageUrl: String,
        val name: String,
        val subtitle: TextReference,
        val onClick: () -> Unit,
    ) : ProviderListItemUM

    data class Unavailable(
        override val providerId: String,
        val imageUrl: String,
        val name: String,
        val subtitle: TextReference,
    ) : ProviderListItemUM
}
