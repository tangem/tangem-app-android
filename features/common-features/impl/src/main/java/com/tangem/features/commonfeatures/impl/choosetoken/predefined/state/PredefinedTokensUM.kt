package com.tangem.features.commonfeatures.impl.choosetoken.predefined.state

import com.tangem.core.ui.extensions.TextReference
import com.tangem.utils.annotations.RemoveWithToggle
import kotlinx.collections.immutable.ImmutableList

internal data class PredefinedTokensUM(
    val items: ImmutableList<PredefinedTokenItemUM>,
    @RemoveWithToggle(toggleName = "AND_18671_REDESIGNED_TOKEN_ROWS_ENABLED", description = "Inline true")
    val isRedesignedTokenRowsEnabled: Boolean = false,
)

internal data class PredefinedTokenItemUM(
    val id: String,
    val symbol: String,
    val networkName: TextReference,
    val networkId: String,
    val iconUrl: String?,
    val onAddClick: () -> Unit,
)