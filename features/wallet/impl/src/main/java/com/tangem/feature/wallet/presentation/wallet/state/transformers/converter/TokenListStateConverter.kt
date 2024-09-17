package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.model.TotalFiatBalance
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState.TokensListItemState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState.OrganizeTokensButtonConfig as WalletOrganizeTokensButtonConfig

internal class TokenListStateConverter(
    appCurrency: AppCurrency,
    private val tokenList: TokenList,
    private val selectedWallet: UserWallet,
    private val clickIntents: WalletClickIntents,
) : Converter<WalletTokensListState, WalletTokensListState> {

    private val tokenStatusConverter = TokenItemStateConverter(
        appCurrency = appCurrency,
        onItemClick = clickIntents::onTokenItemClick,
        onItemLongClick = clickIntents::onTokenItemLongClick,
    )

    override fun convert(value: WalletTokensListState): WalletTokensListState {
        return when (tokenList) {
            is TokenList.Empty -> WalletTokensListState.Empty
            is TokenList.GroupedByNetwork -> WalletTokensListState.ContentState.Content(
                items = tokenList.toGroupedItems(),
                organizeTokensButtonConfig = getOrganizeTokensButtonState(
                    currenciesSize = tokenList.groups.flatMap(NetworkGroup::currencies).size,
                ),
            )
            is TokenList.Ungrouped -> WalletTokensListState.ContentState.Content(
                items = tokenList.toUngroupedItems(),
                organizeTokensButtonConfig = getOrganizeTokensButtonState(currenciesSize = tokenList.currencies.size),
            )
        }
    }

    private fun TokenList.GroupedByNetwork.toGroupedItems(): PersistentList<TokensListItemState> {
        return groups.fold(initial = persistentListOf()) { acc, group ->
            acc.mutate { it.addGroup(group) }
        }
    }

    private fun TokenList.Ungrouped.toUngroupedItems(): PersistentList<TokensListItemState> {
        return currencies.fold(initial = persistentListOf()) { acc, token ->
            acc.mutate { it.addToken(token) }
        }
    }

    private fun MutableList<TokensListItemState>.addGroup(group: NetworkGroup): List<TokensListItemState> {
        val groupTitle = TokensListItemState.NetworkGroupTitle(
            id = group.network.hashCode(),
            name = stringReference(group.network.name),
        )

        add(groupTitle)
        group.currencies.forEach { token -> addToken(token) }

        return this
    }

    private fun MutableList<TokensListItemState>.addToken(token: CryptoCurrencyStatus): List<TokensListItemState> {
        val tokenItemState = tokenStatusConverter.convert(token)
        add(TokensListItemState.Token(tokenItemState))

        return this
    }

    private fun getOrganizeTokensButtonState(currenciesSize: Int): WalletOrganizeTokensButtonConfig? {
        return if (currenciesSize > 1 && !isSingleCurrencyWalletWithToken()) {
            WalletOrganizeTokensButtonConfig(
                isEnabled = tokenList.totalFiatBalance !is TotalFiatBalance.Loading,
                onClick = clickIntents::onOrganizeTokensClick,
            )
        } else {
            null
        }
    }

    private fun isSingleCurrencyWalletWithToken(): Boolean {
        return selectedWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()
    }
}
