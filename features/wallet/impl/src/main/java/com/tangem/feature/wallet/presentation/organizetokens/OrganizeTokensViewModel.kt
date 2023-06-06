package com.tangem.feature.wallet.presentation.organizetokens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensStateHolder.HeaderConfig
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import javax.inject.Inject
import kotlin.properties.Delegates

// FIXME: Implemented with preview data
@HiltViewModel
internal class OrganizeTokensViewModel @Inject constructor() : ViewModel() {

    var router: InnerWalletRouter by Delegates.notNull()

    var uiState: OrganizeTokensStateHolder by mutableStateOf(getInitialState())
        private set

    private fun getInitialState(): OrganizeTokensStateHolder = WalletPreviewData.organizeTokensState.copy(
        itemsState = OrganizeTokensListState.Ungrouped(
            items = WalletPreviewData.draggableTokens,
        ),
        header = HeaderConfig(
            onSortByBalanceClick = { /* no-op */ },
            onGroupByNetworkClick = this::toggleTokensByNetworkGrouping,
        ),
    )

    private fun toggleTokensByNetworkGrouping() {
        val newListState = when (val itemsState = uiState.itemsState) {
            is OrganizeTokensListState.GroupedByNetwork -> OrganizeTokensListState.Ungrouped(
                items = itemsState.items.filterIsInstance<DraggableItem.Token>().toPersistentList(),
            )
            is OrganizeTokensListState.Ungrouped -> OrganizeTokensListState.GroupedByNetwork(
                items = WalletPreviewData.draggableItems,
            )
        }

        uiState = uiState.copy(itemsState = newListState)
    }
}