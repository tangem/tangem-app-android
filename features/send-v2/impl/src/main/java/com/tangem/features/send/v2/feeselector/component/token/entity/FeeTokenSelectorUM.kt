package com.tangem.features.send.v2.feeselector.component.token.entity

import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import kotlinx.collections.immutable.ImmutableList

internal data class FeeTokenSelectorUM(
    val parent: FeeSelectorUM.Content,
    val selectedToken: FeeTokenItemState,
    val tokens: ImmutableList<FeeTokenItemState>,
)