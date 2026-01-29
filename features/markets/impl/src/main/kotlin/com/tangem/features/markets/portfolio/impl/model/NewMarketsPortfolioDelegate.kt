package com.tangem.features.markets.portfolio.impl.model

import arrow.core.getOrElse
import com.tangem.blockchainsdk.compatibility.getTokenIdIfL2Network
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.TextReference
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
import com.tangem.domain.yield.supply.models.YieldSupplyAvailability
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetAvailabilityUseCase
import com.tangem.features.markets.portfolio.impl.loader.PortfolioData
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM.Tokens.AddButtonState
import com.tangem.features.markets.portfolio.impl.ui.state.PortfolioHeader
import com.tangem.features.markets.portfolio.impl.ui.state.PortfolioListItem
import com.tangem.features.markets.portfolio.impl.ui.state.WalletHeader
import com.tangem.utils.extensions.isZero
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList")
internal class NewMarketsPortfolioDelegate @AssistedInject constructor(
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val allAccountSupplier: MultiAccountStatusListSupplier,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCaseV2,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val yieldSupplyGetAvailabilityUseCase: YieldSupplyGetAvailabilityUseCase,
    isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
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
            when (portfolioWithCurrency.flattenAddedCurrency.isEmpty()) {
                false -> emitAll(contentFlow(portfolioWithCurrency).distinctUntilChanged())
                true -> when (portfolioWithCurrency.hasMultiWallets) {
                    true -> emitAll(addFirstTokenFlow())
                    false -> emit(MyPortfolioUM.UnavailableForWallet)
                }
            }
        }

    private fun addFirstTokenFlow(): Flow<MyPortfolioUM> = buttonState.map { state ->
        when (state) {
            AddButtonState.Loading -> MyPortfolioUM.Loading
            AddButtonState.Available -> MyPortfolioUM.AddFirstToken(
                onAddClick = onAddClick,
                addToPortfolioBSConfig = TangemBottomSheetConfig.Empty,
            )
            AddButtonState.Unavailable -> MyPortfolioUM.Unavailable
        }
    }

    private fun contentFlow(portfolio: PortfoliosWithThisCurrency): Flow<MyPortfolioUM.Content> {
        fun Portfolio.actionsFoAccountCurrencies(): List<Flow<Pair<CryptoCurrency, TokenActionsState>>> =
            accountsWithAdded.map { account ->
                fun CryptoCurrencyStatus.actionsFlow(): Flow<Pair<CryptoCurrency, TokenActionsState>> = flow {
                    val yieldSupplyAvailability = yieldSupplyGetAvailabilityUseCase(this@actionsFlow.currency)
                        .getOrElse { YieldSupplyAvailability.Unavailable }
                    emitAll(
                        getCryptoCurrencyActionsUseCase(
                            accountId = account.accountStatus.account.accountId,
                            currency = this@actionsFlow.currency,
                            yieldSupplyAvailability = yieldSupplyAvailability,
                        ).map { actionsState -> actionsState.cryptoCurrencyStatus.currency to actionsState },
                    )
                }
                account.addedCurrency.map { it.actionsFlow() }
            }.flatten()

        val allAddedTokenActions =
            portfolio.portfolios.map { portfolioItem -> portfolioItem.actionsFoAccountCurrencies() }.flatten()

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
                    .find { it.accountsWithAdded.any { account -> account.addedCurrency.isNotEmpty() } }
                    ?.userWallet
                    ?.let { setOf(it.walletId to currency.currency.id) }
                    .orEmpty()
            }
            else -> emptySet()
        }
        return MutableStateFlow(initValue)
            .also { this.expandedHolder = it }
    }

    private fun portfolioWithThisCurrencyFLow(): Flow<PortfoliosWithThisCurrency> =
        allAccountSupplier().map { list -> list.map { it.addedAccountsFlow() } }.flatMapLatest { flows ->
            combine(flows) { portfolios ->
                PortfoliosWithThisCurrency(
                    currencyRawId = currencyRawId,
                    portfolios = portfolios.toList(),
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
            is AccountStatus.Crypto.Portfolio -> this.tokenList.flattenCurrencies()
                .filter { status ->
                    val currencyId = status.currency.id.rawCurrencyId ?: return@filter false
                    getTokenIdIfL2Network(currencyId.value) == currencyRawId.value
                }
            is AccountStatus.Payment -> TODO("[REDACTED_JIRA]")
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
        val uiItems: MutableList<PortfolioListItem> = mutableListOf()

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
            if (isAccountMode) {
                uiItems.add(portfolioItem.userWallet.toWalletHeader())
            } else {
                uiItems.add(portfolioItem.userWallet.toWalletPortfolioHeader())
            }

            portfolioItem.accountsWithAdded.forEach { accountWithAdded ->
                if (accountWithAdded.addedCurrency.isEmpty()) return@forEach
                if (isAccountMode) {
                    val account = accountWithAdded.accountStatus.account
                    uiItems.add(account.toAccountPortfolioHeader())
                }

                accountWithAdded.addedCurrency.forEach { currencyStatus ->
                    val actions = allActions[currencyStatus.currency]?.states.orEmpty()
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

    private fun Account.toAccountPortfolioHeader(): PortfolioHeader = PortfolioHeader(
        id = this.accountId.value,
        state = AccountTitleUM.Account(
            prefixText = TextReference.EMPTY,
            name = this.accountName.toUM().value,
            icon = when (this) {
                is Account.Crypto.Portfolio -> CryptoPortfolioIconConverter.convert(this.icon)
                is Account.Payment -> TODO("[REDACTED_JIRA]")
            },
        ),
    )

    private fun UserWallet.toWalletPortfolioHeader(): PortfolioHeader = PortfolioHeader(
        id = this.walletId.stringValue,
        state = AccountTitleUM.Text(
            title = stringReference(this.name),
        ),
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