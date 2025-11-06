package com.tangem.core.ui.components.tokenlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.components.account.AccountCharIcon
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.account.AccountResIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.internal.GroupTitleItem
import com.tangem.core.ui.components.tokenlist.state.PortfolioTokensListItemUM
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

/**
 * Multi-currency content item
 *
 * @param state           component UI model
 * @param isBalanceHidden flag that shows/hides balance
 * @param modifier        modifier
 *
[REDACTED_AUTHOR]
 */
@Composable
fun TokenListItem(state: TokensListItemUM, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    when (state) {
        is TokensListItemUM.GroupTitle -> PortfolioTokensListItem(state, isBalanceHidden, modifier)
        is TokensListItemUM.Token -> PortfolioTokensListItem(state, isBalanceHidden, modifier)
        is TokensListItemUM.Portfolio -> PortfolioListItem(state, isBalanceHidden, modifier)
        is TokensListItemUM.SearchBar -> {
            SearchBar(state = state.searchBarUM, modifier = modifier.padding(all = 12.dp))
        }
        is TokensListItemUM.Text -> {
            Text(
                text = state.text.resolveReference(),
                modifier = modifier.padding(all = 12.dp),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.body2,
            )
        }
    }
}

@Composable
fun PortfolioListItem(state: TokensListItemUM.Portfolio, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    if (state.isExpanded) {
        ExpandedPortfolioHeader(state = state.tokenItemUM, isCollapsable = state.isCollapsable, modifier = modifier)
    } else {
        TokenItem(
            state = state.tokenItemUM,
            isBalanceHidden = isBalanceHidden,
            modifier = modifier,
        )
    }
}

@Composable
fun PortfolioTokensListItem(state: PortfolioTokensListItemUM, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    when (state) {
        is TokensListItemUM.GroupTitle -> GroupTitleItem(state, modifier)
        is TokensListItemUM.Token -> TokenItem(
            state = state.state,
            isBalanceHidden = isBalanceHidden,
            modifier = modifier,
        )
    }
}

@Composable
fun ExpandedPortfolioHeader(state: TokenItemState, isCollapsable: Boolean, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = { state.onItemClick?.invoke(state) })
            .padding(
                vertical = TangemTheme.dimens.spacing4,
                horizontal = TangemTheme.dimens.spacing12,
            ),
    ) {
        when (val icon = state.iconState) {
            is CurrencyIconState.CryptoPortfolio.Icon -> AccountResIcon(
                resId = icon.resId,
                color = icon.color,
                size = AccountIconSize.ExtraSmall,
            )
            is CurrencyIconState.CryptoPortfolio.Letter -> AccountCharIcon(
                char = icon.char.resolveReference().first(),
                color = icon.color,
                size = AccountIconSize.ExtraSmall,
            )
            is CurrencyIconState.CoinIcon,
            is CurrencyIconState.CustomTokenIcon,
            is CurrencyIconState.Empty,
            is CurrencyIconState.FiatIcon,
            CurrencyIconState.Loading,
            CurrencyIconState.Locked,
            is CurrencyIconState.TokenIcon,
            -> Unit
        }

        SpacerW4()

        when (val titleState = state.titleState) {
            TokenItemState.TitleState.Loading -> Unit
            TokenItemState.TitleState.Locked -> Unit
            is TokenItemState.TitleState.Content -> Text(
                modifier = Modifier.weight(1f),
                text = titleState.text.resolveReference(),
                color = TangemTheme.colors.text.primary1,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = TangemTheme.typography.caption1,
            )
        }

        if (isCollapsable) {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size16),
                painter = painterResource(id = R.drawable.ic_minimize_24),
                tint = TangemTheme.colors.icon.inactive,
                contentDescription = null,
            )
        }
    }
}