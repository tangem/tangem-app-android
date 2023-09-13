package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.common.Provider
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState.OrganizeTokensButtonState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState.TokensListItemState
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
        clickIntents = clickIntents,
    )

    override fun convert(value: TokenList): WalletTokensListState {
        return when (value) {
            is TokenList.NotInitialized -> WalletTokensListState.Loading()
            is TokenList.GroupedByNetwork -> WalletTokensListState.Content(
                items = value.mapToMultiCurrencyItems(),
                organizeTokensButton = value.mapToOrganizeTokensButtonState(),
            )
            is TokenList.Ungrouped -> WalletTokensListState.Content(
                items = value.mapToMultiCurrencyItems(),
                organizeTokensButton = value.mapToOrganizeTokensButtonState(),
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

    private fun TokenList.GroupedByNetwork.mapToOrganizeTokensButtonState(): OrganizeTokensButtonState {
        return getOrganizeTokensButtonState(
            isLoading = totalFiatBalance is TokenList.FiatBalance.Loading,
            currenciesSize = groups.flatMap(NetworkGroup::currencies).size,
        )
    }

    private fun TokenList.Ungrouped.mapToOrganizeTokensButtonState(): OrganizeTokensButtonState {
        return getOrganizeTokensButtonState(
            isLoading = totalFiatBalance is TokenList.FiatBalance.Loading,
            currenciesSize = currencies.size,
        )
    }

    private fun MutableList<TokensListItemState>.addGroup(group: NetworkGroup): List<TokensListItemState> {
        val groupTitle = TokensListItemState.NetworkGroupTitle(
            id = group.network.hashCode(),
            name = stringReference(group.network.name),
        )

        this.add(groupTitle)

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

    private fun getOrganizeTokensButtonState(isLoading: Boolean, currenciesSize: Int): OrganizeTokensButtonState {
        return if (currenciesSize > 1) {
            OrganizeTokensButtonState.Visible(
                isEnabled = !isLoading,
                onClick = clickIntents::onOrganizeTokensClick,
            )
        } else {
            OrganizeTokensButtonState.Hidden
        }
    }
}