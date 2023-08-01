package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.model.TokenStatus
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletTokensListState.TokensListItemState
import com.tangem.feature.wallet.presentation.wallet.utils.LoadingItemsProvider.getLoadingMultiCurrencyTokens
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickCallbacks
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf

internal class TokenListToContentItemsConverter(
    isWalletContentHidden: Boolean,
    fiatCurrencyCode: String,
    fiatCurrencySymbol: String,
    private val clickCallbacks: WalletClickCallbacks,
) : Converter<TokenList, WalletTokensListState> {

    private val tokenStatusConverter = TokenStatusToTokenItemConverter(
        isWalletContentHidden = isWalletContentHidden,
        fiatCurrencyCode = fiatCurrencyCode,
        fiatCurrencySymbol = fiatCurrencySymbol,
    )

    override fun convert(value: TokenList): WalletTokensListState {
        return WalletTokensListState.Content(
            items = when (value) {
                is TokenList.GroupedByNetwork -> value.mapToMultiCurrencyItems()
                is TokenList.Ungrouped -> value.mapToMultiCurrencyItems()
                is TokenList.NotInitialized -> getLoadingMultiCurrencyTokens()
            },
            onOrganizeTokensClick = if (value.totalFiatBalance is TokenList.FiatBalance.Loaded) {
                clickCallbacks::onOrganizeTokensClick
            } else {
                null
            },
        )
    }

    private fun TokenList.GroupedByNetwork.mapToMultiCurrencyItems(): PersistentList<TokensListItemState> {
        return groups.fold(initial = persistentListOf()) { acc, group ->
            acc.mutate { it.addGroup(group) }
        }
    }

    private fun TokenList.Ungrouped.mapToMultiCurrencyItems(): PersistentList<TokensListItemState> {
        return tokens.fold(initial = persistentListOf()) { acc, token ->
            acc.mutate { it.addToken(token) }
        }
    }

    private fun MutableList<TokensListItemState>.addGroup(group: NetworkGroup): List<TokensListItemState> {
        this.add(TokensListItemState.NetworkGroupTitle(group.network.name))

        group.tokens.forEach { token ->
            this.addToken(token)
        }

        return this
    }

    private fun MutableList<TokensListItemState>.addToken(token: TokenStatus): List<TokensListItemState> {
        val tokenItemState = tokenStatusConverter.convert(token)

        this.add(TokensListItemState.Token(tokenItemState))

        return this
    }
}