package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.ui.account.AccountCryptoPortfolioItemStateConverter
import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.ui.components.tokenlist.state.PortfolioTokensListItemUM
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.tokenlist.TokenList.GroupedByNetwork.NetworkGroup
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.transformers.TokenConverterParams
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState.OrganizeTokensButtonConfig as WalletOrganizeTokensButtonConfig

@Suppress("LongParameterList")
internal class TokenListStateConverter(
    private val appCurrency: AppCurrency,
    private val params: TokenConverterParams,
    private val selectedWallet: UserWallet,
    private val clickIntents: WalletClickIntents,
    private val yieldModuleApyMap: Map<String, BigDecimal>,
    private val stakingApyMap: Map<String, List<Yield.Validator>>,
    private val shouldShowMainPromo: Boolean,
) : Converter<WalletTokensListState, WalletTokensListState> {

    private val yieldSupplyPromoBannerKeyConverter = YieldSupplyPromoBannerKeyConverter(
        yieldModuleApyMap,
        shouldShowMainPromo,
    )

    private val onTokenClick: (accountId: AccountId?, currencyStatus: CryptoCurrencyStatus) -> Unit =
        { accountId, currencyStatus ->
            clickIntents.onTokenItemClick(selectedWallet.walletId, currencyStatus)
        }

    private val onTokenLongClick: (accountId: AccountId?, currencyStatus: CryptoCurrencyStatus) -> Unit =
        { accountId, currencyStatus ->
            clickIntents.onTokenItemLongClick(selectedWallet.walletId, currencyStatus)
        }

    private val onApyLabelClick: (currencyStatus: CryptoCurrencyStatus, apy: String) -> Unit =
        { currencyStatus, apy ->
            clickIntents.onApyLabelClick(selectedWallet.walletId, currencyStatus, apy)
        }

    private fun tokenStatusConverter(accountId: AccountId? = null) = TokenItemStateConverter(
        appCurrency = appCurrency,
        yieldModuleApyMap = yieldModuleApyMap,
        yieldSupplyPromoBannerKey = yieldSupplyPromoBannerKeyConverter.convert(params),
        stakingApyMap = stakingApyMap,
        onItemClick = { _, status -> onTokenClick(accountId, status) },
        onItemLongClick = { _, status -> onTokenLongClick(accountId, status) },
        onApyLabelClick = { status, apy -> onApyLabelClick(status, apy) },
        onYieldPromoCloseClick = clickIntents::onYieldPromoCloseClick,
    )

    override fun convert(value: WalletTokensListState): WalletTokensListState {
        return when (params) {
            is TokenConverterParams.Account -> convertAccountList(params)
            is TokenConverterParams.Wallet -> convertTokenList(
                tokenConverter = tokenStatusConverter((params.portfolioId as? PortfolioId.Account)?.accountId),
                tokenList = params.tokenList,
            )
        }
    }

    private fun convertTokenList(tokenConverter: TokenItemStateConverter, tokenList: TokenList): WalletTokensListState =
        when (tokenList) {
            is TokenList.Empty -> WalletTokensListState.Empty
            is TokenList.GroupedByNetwork -> WalletTokensListState.ContentState.Content(
                items = tokenList.toGroupedItems(tokenConverter),
                organizeTokensButtonConfig = getOrganizeTokensButtonState(tokenList = tokenList),
            )
            is TokenList.Ungrouped -> WalletTokensListState.ContentState.Content(
                items = tokenList.toUngroupedItems(tokenConverter),
                organizeTokensButtonConfig = getOrganizeTokensButtonState(tokenList = tokenList),
            )
        }

    private fun convertAccountList(params: TokenConverterParams.Account): WalletTokensListState {
        val accountList = params.accountList

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
                priceChangeLce = this.priceChangeLce,
            )
            val accountItem = converter.convert(tokenList.totalFiatBalance)
            val tokenConverter = tokenStatusConverter(account.accountId)
            val tokensListState = convertTokenList(tokenConverter, tokenList)
            val items = when (tokensListState) {
                is WalletTokensListState.ContentState.PortfolioContent -> tokensListState.items
                is WalletTokensListState.ContentState.Content -> tokensListState.items
                is WalletTokensListState.ContentState.Loading -> tokensListState.items
                is WalletTokensListState.ContentState.Locked -> tokensListState.items
                is WalletTokensListState.Empty -> listOf()
            }
            return TokensListItemUM.Portfolio(
                tokenItemUM = accountItem,
                isExpanded = isExtend,
                isCollapsable = true,
                tokens = items.filterIsInstance<PortfolioTokensListItemUM>().toPersistentList(),
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
            organizeTokensButtonConfig = getOrganizeTokensButtonStateV2(accountList = accountList),
        )
    }

    private fun TokenList.GroupedByNetwork.toGroupedItems(
        tokenConverter: TokenItemStateConverter,
    ): PersistentList<TokensListItemUM> {
        return groups.fold(initial = persistentListOf()) { acc, group ->
            acc.mutate { it.addGroup(tokenConverter, group) }
        }
    }

    private fun TokenList.Ungrouped.toUngroupedItems(
        tokenConverter: TokenItemStateConverter,
    ): PersistentList<TokensListItemUM> {
        return currencies.fold(initial = persistentListOf()) { acc, token ->
            acc.mutate { it.addToken(tokenConverter, token) }
        }
    }

    private fun MutableList<TokensListItemUM>.addGroup(
        tokenConverter: TokenItemStateConverter,
        group: NetworkGroup,
    ): List<TokensListItemUM> {
        val groupTitle = TokensListItemUM.GroupTitle(
            id = group.network.hashCode(),
            text = resourceReference(
                id = R.string.wallet_network_group_title,
                formatArgs = wrappedList(group.network.name),
            ),
        )

        add(groupTitle)
        group.currencies.forEach { token -> addToken(tokenConverter, token) }

        return this
    }

    private fun MutableList<TokensListItemUM>.addToken(
        tokenConverter: TokenItemStateConverter,
        token: CryptoCurrencyStatus,
    ): List<TokensListItemUM> {
        val tokenItemState = tokenConverter.convert(token)

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

    private fun getOrganizeTokensButtonStateV2(accountList: AccountStatusList): WalletOrganizeTokensButtonConfig? {
        return if (accountList.flattenCurrencies().size > 1 && !isSingleCurrencyWalletWithToken()) {
            WalletOrganizeTokensButtonConfig(
                isEnabled = accountList.totalFiatBalance !is TotalFiatBalance.Loading,
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