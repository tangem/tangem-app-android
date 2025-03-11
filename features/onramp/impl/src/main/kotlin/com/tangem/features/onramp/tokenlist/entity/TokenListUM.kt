package com.tangem.features.onramp.tokenlist.entity

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import kotlinx.collections.immutable.ImmutableList

/**
 * Token list UM
 *
 * @property searchBarUM      search bar UI model
 * @property availableItems   available items (search bar, header, tokens)
 * @property unavailableItems unavailable items (header, tokens)
 * @property isBalanceHidden  flag that indicates if balance should be hidden
 *
[REDACTED_AUTHOR]
 */
internal data class TokenListUM(
    val searchBarUM: SearchBarUM,
    val availableItems: ImmutableList<TokensListItemUM>,
    val unavailableItems: ImmutableList<TokensListItemUM>,
    val isBalanceHidden: Boolean,
    val warning: NotificationUM? = null,
)