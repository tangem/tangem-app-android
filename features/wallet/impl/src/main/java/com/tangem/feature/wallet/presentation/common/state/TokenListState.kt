package com.tangem.feature.wallet.presentation.common.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface TokenListState {

    data class GroupedByNetwork(
        val groups: ImmutableList<NetworkGroupState>,
    ) : TokenListState

    data class Ungrouped(
        val tokens: ImmutableList<TokenItemState>,
    ) : TokenListState
}