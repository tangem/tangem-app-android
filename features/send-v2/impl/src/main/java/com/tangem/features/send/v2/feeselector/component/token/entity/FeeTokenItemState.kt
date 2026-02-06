package com.tangem.features.send.v2.feeselector.component.token.entity

import com.tangem.core.ui.components.token.state.TokenItemState

internal data class FeeTokenItemState(
    val state: TokenItemState,
    val isAvailable: Boolean = true,
)