package com.tangem.feature.tester.presentation.providers.entity

import kotlinx.collections.immutable.ImmutableList

internal data class BlockchainProvidersUM(
    val onBackClick: () -> Unit,
    val blockchainProviders: ImmutableList<ProvidersUM>,
) {

    data class ProvidersUM(
        val blockchainId: String,
        val blockchainName: String,
        val blockchainSymbol: String,
        val providers: ImmutableList<ProviderUM>,
        val isExpanded: Boolean,
    )

    data class ProviderUM(val name: String)
}
