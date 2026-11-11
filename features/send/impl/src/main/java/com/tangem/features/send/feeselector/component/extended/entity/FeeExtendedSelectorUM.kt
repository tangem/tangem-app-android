package com.tangem.features.send.feeselector.component.extended.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.utils.annotations.RemoveWithToggle

@Immutable
data class FeeExtendedSelectorUM(
    val parent: FeeSelectorUM.Content,
    val token: TokenItemState,
    val fee: FeeItem,
    val onFeeClick: () -> Unit,
    @RemoveWithToggle(toggleName = "AND_18671_REDESIGNED_TOKEN_ROWS_ENABLED", description = "Inline true")
    val isRedesignedTokenRowsEnabled: Boolean = false,
)