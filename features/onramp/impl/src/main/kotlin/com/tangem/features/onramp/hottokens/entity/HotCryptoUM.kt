package com.tangem.features.onramp.hottokens.entity

import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.utils.annotations.RemoveWithToggle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Hot crypto UM
 *
 * @property items items
 *
[REDACTED_AUTHOR]
 */
internal data class HotCryptoUM(
    val items: ImmutableList<TokensListItemUM>,
    @RemoveWithToggle(toggleName = "AND_18671_REDESIGNED_TOKEN_ROWS_ENABLED", description = "Drop the default")
    val rows: ImmutableList<TangemTokenRow.State> = persistentListOf(),
)