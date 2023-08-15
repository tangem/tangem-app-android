package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.common.Provider
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState.TokensListItemState
import com.tangem.feature.wallet.presentation.wallet.utils.LoadingItemsProvider.getLoadingMultiCurrencyTokens
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf

internal class TokenListToContentItemsConverter(
    appCurrencyProvider: Provider<AppCurrency>,
    isWalletContentHidden: Boolean,
    private val clickIntents: WalletClickIntents,
) : Converter<TokenList, WalletTokensListState> {

    private val tokenStatusConverter = CryptoCurrencyStatusToTokenItemConverter(
        isWalletContentHidden = isWalletContentHidden,
        appCurrencyProvider = appCurrencyProvider,
    )

    override fun convert(value: TokenList): WalletTokensListState {
        val isEmptyList = when (value) {
            is TokenList.GroupedByNetwork -> value.groups.isEmpty()
            is TokenList.NotInitialized -> false
            is TokenList.Ungrouped -> value.currencies.isEmpty()
        }

        return if (isEmptyList) {
            WalletTokensListState.Empty
        } else {
            WalletTokensListState.Content(
                items = when (value) {
                    is TokenList.GroupedByNetwork -> value.mapToMultiCurrencyItems()
                    is TokenList.Ungrouped -> value.mapToMultiCurrencyItems()
                    is TokenList.NotInitialized -> getLoadingMultiCurrencyTokens()
                },
                onOrganizeTokensClick = if (value.totalFiatBalance is TokenList.FiatBalance.Loaded) {
                    clickIntents::onOrganizeTokensClick
                } else {
                    null
                },
            )
        }
    }

    private fun TokenList.GroupedByNetwork.mapToMultiCurrencyItems(): PersistentList<TokensListItemState> {
        return groups.fold(initial = persistentListOf()) { acc, group ->
            acc.mutate { it.addGroup(group) }
        }
    }

    private fun TokenList.Ungrouped.mapToMultiCurrencyItems(): PersistentList<TokensListItemState> {
        return currencies.fold(initial = persistentListOf()) { acc, token ->
            acc.mutate { it.addToken(token) }
        }
    }

    private fun MutableList<TokensListItemState>.addGroup(group: NetworkGroup): List<TokensListItemState> {
        this.add(TokensListItemState.NetworkGroupTitle(TextReference.Str(group.network.name)))

        group.currencies.forEach { token ->
            this.addToken(token)
        }

        return this
    }

    private fun MutableList<TokensListItemState>.addToken(token: CryptoCurrencyStatus): List<TokensListItemState> {
        val tokenItemState = tokenStatusConverter.convert(token)

        this.add(TokensListItemState.Token(tokenItemState))

        return this
    }
}
