package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState
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
        onItemClick = { _, status -> clickIntents.onTokenItemClick(status) },
        onItemLongClick = { _, status -> clickIntents.onTokenItemLongClick(status) },
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

    private fun TokenList.GroupedByNetwork.toGroupedItems(): PersistentList<TokensListItemUM> {
        return groups.fold(initial = persistentListOf()) { acc, group ->
            acc.mutate { it.addGroup(group) }
        }
    }

    private fun TokenList.Ungrouped.toUngroupedItems(): PersistentList<TokensListItemUM> {
        return currencies.fold(initial = persistentListOf()) { acc, token ->
            acc.mutate { it.addToken(token) }
        }
    }

    private fun MutableList<TokensListItemUM>.addGroup(group: NetworkGroup): List<TokensListItemUM> {
        val groupTitle = TokensListItemUM.GroupTitle(
            id = group.network.hashCode(),
            text = resourceReference(
                id = R.string.wallet_network_group_title,
                formatArgs = wrappedList(group.network.name),
            ),
        )

        add(groupTitle)
        group.currencies.forEach { token -> addToken(token) }

        return this
    }

    private fun MutableList<TokensListItemUM>.addToken(token: CryptoCurrencyStatus): List<TokensListItemUM> {
        val tokenItemState = tokenStatusConverter.convert(token)
        add(TokensListItemUM.Token(tokenItemState))

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
        return selectedWallet is UserWallet.Cold &&
            selectedWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()
    }
}