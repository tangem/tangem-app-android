package com.tangem.features.markets.portfolio.impl.model

import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.GetCryptoCurrencyActionsUseCaseV2
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.markets.portfolio.impl.loader.PortfolioData
import com.tangem.features.markets.portfolio.impl.ui.state.AccountHeader
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM.Tokens.AddButtonState
import com.tangem.features.markets.portfolio.impl.ui.state.PortfolioListItem
import com.tangem.features.markets.portfolio.impl.ui.state.WalletHeader
import com.tangem.utils.extensions.isZero
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

@Suppress("LongParameterList")
internal class NewMarketsPortfolioDelegate @AssistedInject constructor(
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val allAccountSupplier: MultiAccountStatusListSupplier,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCaseV2,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    @Assisted private val scope: CoroutineScope,
    @Assisted private val token: TokenMarketParams,
    @Assisted private val tokenActionsHandler: TokenActionsHandler,
    @Assisted private val buttonState: Flow<AddButtonState>,
    @Assisted private val onAddClick: () -> Unit,
) {

    private val currencyRawId: CryptoCurrency.RawID = token.id
    private var expandedHolder: MutableStateFlow<Set<Pair<UserWalletId, CryptoCurrency.ID>>>? = null

    private val settingsFlow: Flow<SettingsBox> = combine(
        flow = getSelectedAppCurrencyUseCase.invokeOrDefault(),
        flow2 = getBalanceHidingSettingsUseCase.isBalanceHidden(),
        flow3 = isAccountsModeEnabledUseCase(),
        transform = ::SettingsBox,
    ).shareIn(
        replay = 1,
        started = SharingStarted.Eagerly,
        scope = scope,
    ).distinctUntilChanged()

    private val availableNetworks = MutableSharedFlow<List<TokenMarketInfo.Network>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun setTokenNetworks(networks: List<TokenMarketInfo.Network>) {
        availableNetworks.tryEmit(networks)
    }

    fun combineData(): Flow<MyPortfolioUM> {
        return availableNetworks.transformLatest { availableNetworks ->
            when {
                availableNetworks.isEmpty() -> emit(MyPortfolioUM.Unavailable)
                else -> emitAll(onAvailableNetworksFlow().distinctUntilChanged())
            }
        }.distinctUntilChanged()
    }

    private fun onAvailableNetworksFlow(): Flow<MyPortfolioUM> =
        portfolioWithThisCurrencyFLow().transformLatest { portfolioWithCurrency ->
            fun addFirstTokenUM() = MyPortfolioUM.AddFirstToken(
                onAddClick = onAddClick,
                addToPortfolioBSConfig = TangemBottomSheetConfig.Empty,
            )
            when (portfolioWithCurrency.flattenAddedCurrency.isEmpty()) {
                false -> emitAll(contentFlow(portfolioWithCurrency).distinctUntilChanged())
                true -> when (portfolioWithCurrency.hasMultiWallets) {
                    true -> emit(addFirstTokenUM())
                    false -> emit(MyPortfolioUM.UnavailableForWallet)
                }
            }
        }

    private fun contentFlow(portfolio: PortfoliosWithThisCurrency): Flow<MyPortfolioUM.Content> {
        fun Portfolio.actionsFoAccountCurrencies(): List<Flow<Pair<CryptoCurrency, TokenActionsState>>> =
            accountsWithAdded.map { account ->
                fun CryptoCurrencyStatus.actionsFlow() = getCryptoCurrencyActionsUseCase(
                    accountId = account.accountStatus.account.accountId,
                    currency = this.currency,
                ).map { actionsState -> actionsState.cryptoCurrencyStatus.currency to actionsState }
                account.addedCurrency.map { it.actionsFlow() }
            }.flatten()

        val allAddedTokenActions =
            portfolio.portfolios.map { portfolio -> portfolio.actionsFoAccountCurrencies() }.flatten()

        return combine(
            flow = combine(allAddedTokenActions) { it.toMap() }.distinctUntilChanged(),
            flow2 = buttonState.distinctUntilChanged(),
            flow3 = getExpandedHolder(portfolio),
            flow4 = settingsFlow.distinctUntilChanged(),
            transform = { actions, addButtonState, expanded, settings ->
                buildContentState(
                    portfolio = portfolio,
                    allActions = actions,
                    addButtonState = addButtonState,
                    expanded = expanded,
                    settings = settings,
                )
            },
        )
    }

    private fun getExpandedHolder(
        portfolio: PortfoliosWithThisCurrency,
    ): StateFlow<Set<Pair<UserWalletId, CryptoCurrency.ID>>> {
        val expandedHolder = this.expandedHolder
        if (expandedHolder != null) return expandedHolder
        val allAddedCurrency = portfolio.flattenAddedCurrency
        val shouldForceExpand = allAddedCurrency.size == 1 &&
            allAddedCurrency.first().value.amount?.isZero() == true

        val initValue = when {
            shouldForceExpand -> {
                val currency = allAddedCurrency.first()
                // find userWallet than have this single added token
                portfolio.portfolios
                    .find { it.accountsWithAdded.find { account -> account.addedCurrency.isNotEmpty() } != null }
                    ?.userWallet
                    ?.let { setOf(it.walletId to currency.currency.id) }
                    ?: setOf()
            }
            else -> setOf()
        }
        return MutableStateFlow(initValue)
            .also { this.expandedHolder = it }
    }

    private fun portfolioWithThisCurrencyFLow(): Flow<PortfoliosWithThisCurrency> =
        allAccountSupplier(Unit).map { list -> list.map { it.addedAccountsFlow() } }.flatMapLatest { flows ->
            combine(flows) {
                PortfoliosWithThisCurrency(
                    currencyRawId = currencyRawId,
                    portfolios = it.toList(),
                )
            }
        }.distinctUntilChanged()

    private fun AccountStatusList.addedAccountsFlow(): Flow<Portfolio> =
        getUserWalletUseCase.invokeFlow(this.userWalletId).mapNotNull { it.getOrNull() }.map { wallet ->
            Portfolio(
                userWallet = wallet,
                accountStatusList = this,
                accountsWithAdded = this.filterByRawID(),
            )
        }.distinctUntilChanged()

    private fun AccountStatusList.filterByRawID(): List<AccountWithAdded> {
        fun AccountStatus.filterByRawID(): List<CryptoCurrencyStatus> = when (this) {
            is AccountStatus.CryptoPortfolio -> this.tokenList.flattenCurrencies()
                .filter { status -> status.currency.id.rawCurrencyId == currencyRawId }
        }
        return accountStatuses.map { accountStatus ->
            AccountWithAdded(
                accountStatus = accountStatus,
                addedCurrency = accountStatus.filterByRawID(),
            )
        }
    }

    private fun buildContentState(
        portfolio: PortfoliosWithThisCurrency,
        allActions: Map<CryptoCurrency, TokenActionsState>,
        addButtonState: AddButtonState,
        expanded: Set<Pair<UserWalletId, CryptoCurrency.ID>>,
        settings: SettingsBox,
    ): MyPortfolioUM.Content {
        val appCurrency = settings.appCurrency
        val isBalanceHidden = settings.isBalanceHidden
        val isAccountMode = settings.isAccountMode
        val uiItems: MutableList<PortfolioListItem> = mutableListOf<PortfolioListItem>()

        fun toggleQuickActions(key: Pair<UserWalletId, CryptoCurrency.ID>) = expandedHolder?.update { expanded ->
            val isExpand = expanded.contains(key)
            if (isExpand) expanded.minus(key) else expanded.plus(key)
        }

        val tokenUMConverter = PortfolioTokenUMConverter(
            appCurrency = appCurrency,
            isBalanceHidden = isBalanceHidden,
            onTokenItemClick = { },
            tokenActionsHandler = tokenActionsHandler,
        )

        portfolio.portfolios.forEach { portfolioItem ->
            if (portfolioItem.flattenAddedCurrency.isEmpty()) return@forEach
            val userWallet = portfolioItem.userWallet
            uiItems.add(portfolioItem.userWallet.toWalletHeader())

            portfolioItem.accountsWithAdded.forEach { accountWithAdded ->
                if (accountWithAdded.addedCurrency.isEmpty()) return@forEach
                if (isAccountMode) {
                    val account = accountWithAdded.accountStatus.account
                    uiItems.add(account.toAccountHeader())
                }

                accountWithAdded.addedCurrency.forEach { currencyStatus ->
                    val actions = allActions[currencyStatus.currency]?.states
                        ?: emptyList()
                    val value = PortfolioData.CryptoCurrencyData(
                        userWallet = userWallet,
                        status = currencyStatus,
                        actions = actions,
                    )
                    val expandedKey = portfolioItem.userWallet.walletId to currencyStatus.currency.id
                    val isExpand = expanded.contains(expandedKey)

                    val tokenItem = tokenUMConverter.convertV2(
                        onTokenItemClick = { wallet, status ->
                            toggleQuickActions(wallet.walletId to status.currency.id)
                        },
                        value = value,
                        isQuickActionsShown = isExpand,
                    )
                    uiItems.add(tokenItem)
                }
            }
        }

        return MyPortfolioUM.Content(
            items = uiItems.toImmutableList(),
            buttonState = addButtonState,
            onAddClick = onAddClick,
        )
    }

    private fun Account.toAccountHeader(): AccountHeader = AccountHeader(
        id = this.accountId.value,
        name = this.accountName.toUM().value,
        icon = when (this) {
            is Account.CryptoPortfolio -> this.icon.toUM()
        },
    )

    private fun UserWallet.toWalletHeader(): WalletHeader = WalletHeader(
        id = this.walletId.stringValue,
        name = stringReference(this.name),
    )

    @Suppress("LongParameterList")
    @AssistedFactory
    interface Factory {
        fun create(
            scope: CoroutineScope,
            token: TokenMarketParams,
            tokenActionsHandler: TokenActionsHandler,
            buttonState: Flow<AddButtonState>,
            onAddClick: () -> Unit,
        ): NewMarketsPortfolioDelegate
    }
}

private data class PortfoliosWithThisCurrency(
    val currencyRawId: CryptoCurrency.RawID,
    val portfolios: List<Portfolio>,
) {

    val hasMultiWallets: Boolean = portfolios.any { it.userWallet.isMultiCurrency }

    val flattenAddedCurrency: List<CryptoCurrencyStatus> =
        portfolios.map { portfolio -> portfolio.flattenAddedCurrency }.flatten()
}

private data class Portfolio(
    val userWallet: UserWallet,
    val accountStatusList: AccountStatusList,
    val accountsWithAdded: List<AccountWithAdded>,
) {
    val flattenAddedCurrency: List<CryptoCurrencyStatus> =
        accountsWithAdded.map { it.addedCurrency }.flatten()
}

private data class AccountWithAdded(
    val addedCurrency: List<CryptoCurrencyStatus>,
    val accountStatus: AccountStatus,
)

private data class SettingsBox(
    val appCurrency: AppCurrency,
    val isBalanceHidden: Boolean,
    val isAccountMode: Boolean,
)