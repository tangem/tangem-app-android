package com.tangem.feature.wallet.presentation.common.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface NetworkGroupState {
    val id: String
    val networkName: String

    data class Draggable(
        override val id: String,
        override val networkName: String,
    ) : NetworkGroupState

    data class Content(
        override val id: String,
        override val networkName: String,
        val tokens: ImmutableList<TokenItemState>,
    ) : NetworkGroupState
}