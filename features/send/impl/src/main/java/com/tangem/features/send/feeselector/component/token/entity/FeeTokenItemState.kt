package com.tangem.features.send.feeselector.component.token.entity

import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow

internal data class FeeTokenItemState(
    val state: TokenItemState,
    val isAvailable: Boolean = true,
    val tokenRow: TangemTokenRow.State? = null,
)