package com.tangem.common.ui.account

import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.state.PortfolioItemContentUM
import com.tangem.core.ui.components.tokenlist.state.PortfolioTokensListItemUM
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList

class TokensListPortfolioItemConverter(
    val tokenItemUM: TokenItemState,
    val isExpanded: Boolean,
    val isCollapsable: Boolean,
    val tokens: ImmutableList<PortfolioTokensListItemUM>,
    val onEmptyAction: PortfolioItemContentUM.Empty.Action? = null,
) : Converter<Unit, TokensListItemUM.Portfolio> {

    override fun convert(value: Unit): TokensListItemUM.Portfolio {
        val content = if (tokens.isEmpty()) {
            PortfolioItemContentUM.Empty(onEmptyAction)
        } else {
            PortfolioItemContentUM.Tokens(tokens)
        }
        return TokensListItemUM.Portfolio(
            tokenItemUM = tokenItemUM,
            isExpanded = isExpanded,
            isCollapsable = isCollapsable,
            content = content,
        )
    }
}