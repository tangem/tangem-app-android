package com.tangem.features.onramp.providers.entity

import com.tangem.core.ui.extensions.TextReference

internal sealed interface ProviderListItemUM {

    val providerId: String
    val name: String
    val imageUrl: String

    sealed interface Available : ProviderListItemUM {
        val onClick: () -> Unit
        val providerResult: SelectProviderResult
        val isBestRate: Boolean
        val isSelected: Boolean

        data class Content(
            override val providerId: String,
            override val name: String,
            override val onClick: () -> Unit,
            override val imageUrl: String,
            override val providerResult: SelectProviderResult,
            override val isBestRate: Boolean,
            override val isSelected: Boolean,
            val rate: String,
            val diffRate: TextReference?,
        ) : Available

        data class WithError(
            override val providerId: String,
            override val name: String,
            override val onClick: () -> Unit,
            override val imageUrl: String,
            override val providerResult: SelectProviderResult,
            override val isBestRate: Boolean,
            override val isSelected: Boolean,
            val subtitle: TextReference,
        ) : Available
    }

    data class Unavailable(
        override val providerId: String,
        override val name: String,
        override val imageUrl: String,
        val subtitle: TextReference,
    ) : ProviderListItemUM
}