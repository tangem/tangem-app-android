package com.tangem.feature.tester.presentation.providers.entity

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.feature.tester.presentation.common.components.appbar.TopBarWithRefreshUM
import kotlinx.collections.immutable.ImmutableList

internal data class BlockchainProvidersUM(
    val topBar: TopBarWithRefreshUM,
    val blockchainProviders: ImmutableList<ProvidersUM>,
) {

    @Immutable
    data class ProvidersUM(
        val blockchainId: String,
        val blockchainName: String,
        val blockchainSymbol: String,
        val providers: ImmutableList<ProviderUM>,
        val isExpanded: Boolean,
        val onDrop: (id: String, prev: Int, current: Int) -> Unit,
    )

    @Immutable
    data class ProviderUM(val type: ProviderType) {

        val name: String = when (type) {
            is ProviderType.Public -> type.url
            else -> type::class.java.simpleName
        }
    }
}