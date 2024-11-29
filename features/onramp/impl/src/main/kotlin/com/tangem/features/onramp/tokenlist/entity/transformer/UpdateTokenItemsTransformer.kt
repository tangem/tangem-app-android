package com.tangem.features.onramp.tokenlist.entity.transformer

import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.common.ui.tokens.TokenItemStateConverter.Companion.getFormattedFiatAmount
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMTransformer
import kotlinx.collections.immutable.toImmutableList

@Suppress("LongParameterList")
internal class UpdateTokenItemsTransformer(
    private val appCurrency: AppCurrency,
    private val onItemClick: (TokenItemState, CryptoCurrencyStatus) -> Unit,
    private val statuses: Map<Boolean, List<CryptoCurrencyStatus>>,
    private val isBalanceHidden: Boolean,
    private val hasSearchBar: Boolean,
    private val unavailableTokensHeaderReference: TextReference,
    private val onQueryChange: (String) -> Unit,
    private val onActiveChange: (Boolean) -> Unit,
) : TokenListUMTransformer {

    override fun transform(prevState: TokenListUM): TokenListUM {
        val availableItems = convertStatuses(
            converter = createDefaultTokenItemStateConverter(),
            statuses = statuses[true].orEmpty(),
        )

        val unavailableItems = convertStatuses(
            converter = createUnavailableTokenItemStateConverter(),
            statuses = statuses[false].orEmpty(),
        )

        val searchBarItem = if (hasSearchBar) {
            prevState.getSearchBar() ?: createSearchBarItem()
        } else {
            null
        }

        return prevState.copy(
            availableItems = buildList {
                if (searchBarItem != null) {
                    add(searchBarItem)
                }

                if (availableItems.isNotEmpty()) {
                    createGroupTitle(
                        textReference = resourceReference(id = R.string.exchange_tokens_available_tokens_header),
                    )
                        .let(::add)
                }

                addAll(availableItems)
            }
                .toImmutableList(),
            unavailableItems = buildList {
                if (unavailableItems.isNotEmpty()) {
                    createGroupTitle(textReference = unavailableTokensHeaderReference).let(::add)
                }

                addAll(unavailableItems)
            }.toImmutableList(),
            isBalanceHidden = isBalanceHidden,
        )
    }

    private fun convertStatuses(
        converter: TokenItemStateConverter,
        statuses: List<CryptoCurrencyStatus>,
    ): List<TokensListItemUM.Token> {
        return converter.convertList(statuses)
            .map(TokensListItemUM::Token)
    }

    private fun createDefaultTokenItemStateConverter(): TokenItemStateConverter {
        return TokenItemStateConverter(appCurrency = appCurrency, onItemClick = onItemClick)
    }

    private fun createUnavailableTokenItemStateConverter(): TokenItemStateConverter {
        return TokenItemStateConverter(
            appCurrency = appCurrency,
            iconStateProvider = { CryptoCurrencyToIconStateConverter(isAvailable = false).convert(it) },
            titleStateProvider = {
                TokenItemState.TitleState.Content(
                    text = stringReference(value = it.currency.name),
                    isAvailable = false,
                )
            },
            subtitleStateProvider = {
                when (it.value) {
                    CryptoCurrencyStatus.Loading -> TokenItemState.SubtitleState.Loading
                    else -> {
                        TokenItemState.SubtitleState.TextContent(
                            value = stringReference(value = it.currency.symbol),
                            isAvailable = false,
                        )
                    }
                }
            },
            fiatAmountStateProvider = {
                when (it.value) {
                    is CryptoCurrencyStatus.Loaded,
                    is CryptoCurrencyStatus.Custom,
                    is CryptoCurrencyStatus.NoQuote,
                    is CryptoCurrencyStatus.NoAccount,
                    -> {
                        TokenItemState.FiatAmountState.TextContent(
                            text = it.getFormattedFiatAmount(appCurrency),
                            isAvailable = false,
                        )
                    }
                    is CryptoCurrencyStatus.Unreachable,
                    is CryptoCurrencyStatus.NoAmount,
                    is CryptoCurrencyStatus.MissedDerivation,
                    is CryptoCurrencyStatus.Loading,
                    -> null
                }
            },
        )
    }

    private fun createSearchBarItem(): TokensListItemUM.SearchBar {
        return TokensListItemUM.SearchBar(
            searchBarUM = SearchBarUM(
                placeholderText = resourceReference(id = R.string.common_search),
                query = "",
                onQueryChange = onQueryChange,
                isActive = false,
                onActiveChange = onActiveChange,
            ),
        )
    }

    private fun createGroupTitle(textReference: TextReference): TokensListItemUM.GroupTitle {
        return TokensListItemUM.GroupTitle(id = textReference.hashCode(), text = textReference)
    }
}