package com.tangem.features.onramp.tokenlist.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal class PreviewTokenListUMProvider : PreviewParameterProvider<TokenListUM> {

    override val values: Sequence<TokenListUM> = sequenceOf(
        createTokensList(
            hasSearchBar = false,
            createHeader(),
            createDefaultTokenItem(),
            createDefaultTokenItem(),
        ),
        createTokensList(
            hasSearchBar = true,
            createHeader(),
            createDefaultTokenItem(),
            createDefaultTokenItem(),
        ),
    )

    private fun createTokensList(hasSearchBar: Boolean, vararg items: TokensListItemUM): TokenListUM {
        return TokenListUM(
            availableItems = buildList {
                if (hasSearchBar) {
                    TokensListItemUM.SearchBar(
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
            unavailableItems = persistentListOf(
                createHeader(),
                createUnavailableTokenItem(),
                createUnavailableTokenItem(),
            ),
            isBalanceHidden = false,
        )
    }

    private fun createHeader(): TokensListItemUM.GroupTitle {
        return TokensListItemUM.GroupTitle(
            id = "exchange_tokens_available_tokens_header",
            text = resourceReference(R.string.exchange_tokens_available_tokens_header),
        )
    }

    private fun createDefaultTokenItem(): TokensListItemUM.Token {
        return TokensListItemUM.Token(
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

    private fun createUnavailableTokenItem(): TokensListItemUM.Token {
        return TokensListItemUM.Token(
            state = TokenItemState.Content(
                id = "2",
                iconState = CurrencyIconState.Locked,
                titleState = TokenItemState.TitleState.Content(text = "Bitcoin", isAvailable = false),
                fiatAmountState = TokenItemState.FiatAmountState.TextContent(
                    text = "12 368,14 \$",
                    isAvailable = false,
                ),
                subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "0,35853044 BTC"),
                subtitleState = TokenItemState.SubtitleState.TextContent(value = "BTC", isAvailable = false),
                onItemClick = {},
                onItemLongClick = {},
            ),
        )
    }
}
