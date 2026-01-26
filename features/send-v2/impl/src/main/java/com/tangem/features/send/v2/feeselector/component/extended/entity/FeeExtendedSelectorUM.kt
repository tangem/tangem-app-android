package com.tangem.features.send.v2.feeselector.component.extended.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.features.send.v2.api.entity.FeeItem
import com.tangem.features.send.v2.api.entity.FeeSelectorUM

@Immutable
data class FeeExtendedSelectorUM(
    val parent: FeeSelectorUM.Content,
    val token: TokenItemState,
    val fee: FeeItem,
    val onFeeClick: () -> Unit,
)