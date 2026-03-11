package com.tangem.features.onramp.tokenlist.entity

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.features.onramp.swap.availablepairs.market.state.SwapMarketState
import kotlinx.collections.immutable.ImmutableList

/**
 * Token list UM
 *
 * @property searchBarUM      search bar UI model
 * @property availableItems   available items (search bar, header, tokens)
 * @property unavailableItems unavailable items (header, tokens)
 * @property isBalanceHidden  flag that indicates if balance should be hidden
 * @property marketsState     markets list state (null when markets should not be shown)
 *
[REDACTED_AUTHOR]
 */
internal data class TokenListUM(
    val searchBarUM: SearchBarUM,
    val availableItems: ImmutableList<TokensListItemUM>,
    val unavailableItems: ImmutableList<TokensListItemUM>,
    val tokensListData: TokenListUMData,
    val isBalanceHidden: Boolean,
    val warning: NotificationUM? = null,
    val marketsState: SwapMarketState? = null,
)

internal sealed interface TokenListUMData {

    val totalTokensCount: Int

    data class AccountList(
        val tokensList: ImmutableList<TokensListItemUM.Portfolio>,
        override val totalTokensCount: Int,
    ) : TokenListUMData

    data class TokenList(
        val tokensList: ImmutableList<TokensListItemUM>,
        override val totalTokensCount: Int,
    ) : TokenListUMData

    data object EmptyList : TokenListUMData {
        override val totalTokensCount: Int = 0
    }
}