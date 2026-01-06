package com.tangem.features.send.v2.feeselector.component.token.entity

import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import kotlinx.collections.immutable.ImmutableList

data class FeeTokenSelectorUM(
    val parent: FeeSelectorUM.Content,
    val selectedToken: TokenItemState,
    val tokens: ImmutableList<TokenItemState>,
)