package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.ui.account.AccountCryptoPortfolioItemStateConverter
import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.ui.components.tokenlist.state.PortfolioTokensListItemUM
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.tokenlist.TokenList.GroupedByNetwork.NetworkGroup
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState.OrganizeTokensButtonConfig
import com.tangem.feature.wallet.presentation.wallet.state.transformers.TokenConverterParams
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState.OrganizeTokensButtonConfig as WalletOrganizeTokensButtonConfig

internal class TokenListStateConverter(
    private val appCurrency: AppCurrency,
    private val params: TokenConverterParams,
    private val selectedWallet: UserWallet,
    private val clickIntents: WalletClickIntents,
) : Converter<WalletTokensListState, WalletTokensListState> {

    private val tokenStatusConverter = when (params) {
        is TokenConverterParams.Account, // todo account Click with accountId param
        is TokenConverterParams.Wallet,
        -> TokenItemStateConverter(
            appCurrency = appCurrency,
            onItemClick = { _, status -> clickIntents.onTokenItemClick(status) },
            onItemLongClick = { _, status -> clickIntents.onTokenItemLongClick(status) },
        )
    }

    override fun convert(value: WalletTokensListState): WalletTokensListState {
        return when (params) {
            is TokenConverterParams.Account -> convertAccountList(params)
            is TokenConverterParams.Wallet -> convertTokenList(params.tokenList)
        }
    }

    private fun convertTokenList(tokenList: TokenList): WalletTokensListState = when (tokenList) {
        is TokenList.Empty -> WalletTokensListState.Empty
        is TokenList.GroupedByNetwork -> WalletTokensListState.ContentState.Content(
            items = tokenList.toGroupedItems(),
            organizeTokensButtonConfig = getOrganizeTokensButtonState(tokenList = tokenList),
        )
        is TokenList.Ungrouped -> WalletTokensListState.ContentState.Content(
            items = tokenList.toUngroupedItems(),
            organizeTokensButtonConfig = getOrganizeTokensButtonState(tokenList = tokenList),
        )
    }

    private fun convertAccountList(params: TokenConverterParams.Account): WalletTokensListState {
        val accountList = params.accountList
        // todo account null for firs iteration?
        val organizeTokensButtonConfig: OrganizeTokensButtonConfig? = null

        fun AccountStatus.CryptoPortfolio.map(): TokensListItemUM.Portfolio {
            val tokenList: TokenList = this.tokenList
            val account: Account.CryptoPortfolio = this.account
            val isExtend = params.expandedAccounts.contains(account.accountId)
            val onItemClick: (Account.CryptoPortfolio) -> Unit = {
                if (isExtend) {
                    clickIntents.onAccountCollapseClick(it)
                } else {
                    clickIntents.onAccountExpandClick(it)
                }
            }
            val converter = AccountCryptoPortfolioItemStateConverter(
                appCurrency = appCurrency,
                account = account,
                onItemClick = onItemClick,
            )
            val accountItem = converter.convert(tokenList.totalFiatBalance)
            val tokensListState = convertTokenList(tokenList)
            val items = when (tokensListState) {
                is WalletTokensListState.ContentState.PortfolioContent -> tokensListState.items
                is WalletTokensListState.ContentState.Content -> tokensListState.items
                is WalletTokensListState.ContentState.Loading -> tokensListState.items
                is WalletTokensListState.ContentState.Locked -> tokensListState.items
                is WalletTokensListState.Empty -> listOf()
            }
            return TokensListItemUM.Portfolio(
                state = accountItem,
                isExpanded = isExtend,
                tokens = items.filterIsInstance<PortfolioTokensListItemUM>(),
            )
        }

        val accountItems = accountList.accountStatuses
            .map { accountStatus ->
                when (accountStatus) {
                    is AccountStatus.CryptoPortfolio -> accountStatus.map()
                }
            }
        return WalletTokensListState.ContentState.PortfolioContent(
            items = accountItems.toPersistentList(),
            organizeTokensButtonConfig = organizeTokensButtonConfig,
        )
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

    private fun getOrganizeTokensButtonState(tokenList: TokenList): WalletOrganizeTokensButtonConfig? {
        val currenciesSize = when (tokenList) {
            TokenList.Empty -> return null
            is TokenList.GroupedByNetwork -> tokenList.groups.flatMap(NetworkGroup::currencies).size
            is TokenList.Ungrouped -> tokenList.currencies.size
        }
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