package com.tangem.feature.wallet.presentation.tokenlist.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.tokenlist.entity.TokenListUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState.TokensListItemState
import kotlinx.collections.immutable.toImmutableList

internal class PreviewTokenListUMProvider : PreviewParameterProvider<TokenListUM> {

    override val values: Sequence<TokenListUM> = sequenceOf(
        createTokensList(hasSearchBar = false, createDefaultTokenItem()),
        createTokensList(hasSearchBar = true, createDefaultTokenItem()),
    )

    private fun createTokensList(hasSearchBar: Boolean, vararg items: TokensListItemState): TokenListUM {
        return TokenListUM(
            items = buildList {
                if (hasSearchBar) {
                    TokensListItemState.SearchBar(
                        searchBarUM = SearchBarUM(
                            placeholderText = resourceReference(id = R.string.common_search),
                            query = "",
                            onQueryChange = {},
                            isActive = false,
                            onActiveChange = {},
                        ),
                    )
                        .let { add(element = it) }
                }

                addAll(items)
            }
                .toImmutableList(),
            isBalanceHidden = false,
        )
    }

    private fun createDefaultTokenItem(): TokensListItemState.Token {
        return TokensListItemState.Token(
            state = TokenItemState.Content(
                id = "1",
                iconState = CurrencyIconState.Locked,
                titleState = TokenItemState.TitleState.Content(text = "Bitcoin"),
                fiatAmountState = TokenItemState.FiatAmountState.Content(text = "12 368,14 \$"),
                subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "0,35853044 BTC"),
                subtitleState = TokenItemState.SubtitleState.CryptoPriceContent(
                    price = "34 496,75 \$",
                    priceChangePercent = "0,43 %",
                    type = PriceChangeType.DOWN,
                ),
                onItemClick = {},
                onItemLongClick = {},
            ),
        )
    }
}