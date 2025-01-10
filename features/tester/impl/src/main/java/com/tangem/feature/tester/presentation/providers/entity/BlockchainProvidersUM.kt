package com.tangem.feature.tester.presentation.providers.entity

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.feature.tester.presentation.common.components.appbar.TopBarWithRefreshUM
import kotlinx.collections.immutable.ImmutableList

internal data class BlockchainProvidersUM(
    val topBar: TopBarWithRefreshUM,
    val searchBar: SearchBarUM,
    val blockchainProviders: ImmutableList<ProvidersUM>,
) {

    @Immutable
    data class ProvidersUM(
        val blockchainId: String,
        val blockchainName: String,
        val blockchainSymbol: String,
        val providers: ImmutableList<ProviderUM>,
        val isExpanded: Boolean,
        val onDrop: (prev: Int, current: Int) -> Unit,
        val addPublicProviderDialog: AddPublicProviderDialogUM,
    )

    @Immutable
    data class ProviderUM(val type: ProviderType) {

        val name: String = when (type) {
            is ProviderType.Public -> type.url
            else -> type::class.java.simpleName
        }
    }

    data class AddPublicProviderDialogUM(
        val hasError: Boolean,
        val onValueChange: (String) -> Unit,
        val onSaveClick: (String) -> Unit,
    )
}