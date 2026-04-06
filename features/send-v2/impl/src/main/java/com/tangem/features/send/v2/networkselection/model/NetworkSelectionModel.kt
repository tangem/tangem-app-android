package com.tangem.features.send.v2.networkselection.model

import androidx.compose.runtime.Stable
import com.tangem.common.getTotalCryptoAmount
import com.tangem.common.getTotalFiatAmount
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.features.send.v2.send.analytics.SendAnalyticEvents
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.common.ui.account.AccountIconItemStateConverter
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.currency.icon.CurrencyIconStateBuilder
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.common.ui.account.toUM
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.send.v2.api.NetworkSelectionComponent
import com.tangem.features.send.v2.networkselection.entity.AccountGroupUM
import com.tangem.features.send.v2.networkselection.entity.NetworkSelectionUM
import com.tangem.features.send.v2.networkselection.entity.WalletGroupUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@Stable
@ModelScoped
internal class NetworkSelectionModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
) : Model() {

    private val params: NetworkSelectionComponent.Params = paramsContainer.require()

    init {
        analyticsEventHandler.send(SendAnalyticEvents.ChooseTokenScreenOpened())
    }

    private val searchQuery = MutableStateFlow("")
    private val expandedWallets = MutableStateFlow(
        params.walletGroups.map { it.userWalletId }.toSet(),
    )

    val uiState: StateFlow<NetworkSelectionUM> = createUiStateFlow()

    private fun createUiStateFlow(): StateFlow<NetworkSelectionUM> {
        val appCurrencyFlow = getSelectedAppCurrencyUseCase.invokeOrDefault()
        val statusListFlow = multiAccountStatusListSupplier()
        val balanceHidingFlow = getBalanceHidingSettingsUseCase()

        return combine(
            flow = searchQuery,
            flow2 = expandedWallets,
            flow3 = appCurrencyFlow,
            flow4 = statusListFlow,
            flow5 = balanceHidingFlow,
        ) { query, expanded, appCurrency, statusLists, balanceHidingSettings ->
            buildState(
                query = query,
                expandedWallets = expanded,
                appCurrency = appCurrency,
                statusLists = statusLists,
                isBalanceHidden = balanceHidingSettings.isBalanceHidden,
            )
        }.stateIn(
            scope = modelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = buildInitialState(),
        )
    }

    private fun buildState(
        query: String,
        expandedWallets: Set<UserWalletId>,
        appCurrency: AppCurrency,
        statusLists: List<AccountStatusList>,
        isBalanceHidden: Boolean,
    ): NetworkSelectionUM {
        val walletGroups = params.walletGroups.mapNotNull { walletGroup ->
            buildWalletGroup(
                walletGroup = walletGroup,
                query = query,
                expandedWallets = expandedWallets,
                appCurrency = appCurrency,
                statusLists = statusLists,
            )
        }.toImmutableList()

        return NetworkSelectionUM(
            searchBar = createSearchBar(query),
            walletGroups = walletGroups,
            isBalanceHidden = isBalanceHidden,
        )
    }

    private fun buildWalletGroup(
        walletGroup: NetworkSelectionComponent.Params.WalletGroup,
        query: String,
        expandedWallets: Set<UserWalletId>,
        appCurrency: AppCurrency,
        statusLists: List<AccountStatusList>,
    ): WalletGroupUM? {
        val statusList = statusLists.find { it.userWalletId == walletGroup.userWalletId }
        val tokenBuildContext = TokenMappingParams(
            userWalletId = walletGroup.userWalletId,
            appCurrency = appCurrency,
            statusMap = buildStatusMap(statusList),
        )

        val accounts = walletGroup.accounts.mapNotNull { accountGroup ->
            val iconState = getAccountIcon(accountGroup.accountId, statusList)
            buildAccountGroup(
                accountGroup = accountGroup,
                query = query,
                context = tokenBuildContext,
                iconState = iconState,
            )
        }.toImmutableList()

        if (accounts.isEmpty()) return null

        return WalletGroupUM(
            userWalletId = walletGroup.userWalletId,
            walletName = walletGroup.walletName,
            isExpanded = walletGroup.userWalletId in expandedWallets,
            onExpandToggle = { toggleWalletExpanded(walletGroup.userWalletId) },
            accounts = accounts,
        )
    }

    private fun buildStatusMap(statusList: AccountStatusList?): Map<CryptoCurrency.ID, CryptoCurrencyStatus> {
        if (statusList == null) return emptyMap()
        return statusList.accountStatuses
            .filterCryptoPortfolio()
            .flatMap { it.flattenCurrencies() }
            .associateBy { it.currency.id }
    }

    private fun buildAccountGroup(
        accountGroup: NetworkSelectionComponent.Params.AccountGroup,
        query: String,
        context: TokenMappingParams,
        iconState: CurrencyIconState.CryptoPortfolio?,
    ): AccountGroupUM? {
        val tokens = accountGroup.currencies
            .filter { matchesQuery(it, query) }
            .map { currency -> buildTokenItem(currency, context) }
            .toImmutableList()

        if (tokens.isEmpty()) return null

        return AccountGroupUM(
            accountName = accountGroup.accountName.toUM().value,
            iconState = iconState,
            tokens = tokens,
            hiddenTokensCount = accountGroup.hiddenTokensCount,
        )
    }

    private fun getAccountIcon(
        accountId: AccountId,
        statusList: AccountStatusList?,
    ): CurrencyIconState.CryptoPortfolio? {
        if (statusList == null) return null
        val account = statusList.accountStatuses
            .filterCryptoPortfolio()
            .find { it.accountId == accountId }
            ?.account ?: return null
        return AccountIconItemStateConverter(size = AccountIconSize.ExtraSmall).convert(account)
    }

    private fun matchesQuery(currency: CryptoCurrency, query: String): Boolean {
        if (query.isBlank()) return true
        val lowerQuery = query.lowercase()
        return currency.name.lowercase().contains(lowerQuery) ||
            currency.symbol.lowercase().contains(lowerQuery) ||
            currency.network.name.lowercase().contains(lowerQuery)
    }

    private fun buildTokenItem(currency: CryptoCurrency, context: TokenMappingParams): TokenItemState {
        val status = context.statusMap[currency.id]
        if (status == null) {
            return TokenItemState.Loading(id = currency.id.value)
        }

        val cryptoAmount = status.getTotalCryptoAmount()
        val fiatAmount = status.getTotalFiatAmount()
        val isFlickering = status.value.sources.total == StatusSource.CACHE

        return TokenItemState.Content(
            id = currency.id.value,
            iconState = CurrencyIconStateBuilder.build(currency),
            titleState = TokenItemState.TitleState.Content(
                text = stringReference(currency.name),
            ),
            subtitleState = TokenItemState.SubtitleState.TextContent(
                value = stringReference(currency.network.name),
            ),
            fiatAmountState = TokenItemState.FiatAmountState.Content(
                text = fiatAmount.format {
                    fiat(
                        fiatCurrencyCode = context.appCurrency.code,
                        fiatCurrencySymbol = context.appCurrency.symbol,
                    )
                },
                isFlickering = isFlickering,
            ),
            subtitle2State = TokenItemState.Subtitle2State.TextContent(
                text = cryptoAmount.format { crypto(currency) },
                isFlickering = isFlickering,
            ),
            onItemClick = {
                analyticsEventHandler.send(
                    SendAnalyticEvents.TokenSelected(
                        token = currency.symbol,
                        blockchain = currency.network.name,
                    ),
                )
                params.onTokenSelected(context.userWalletId, currency)
            },
            onItemLongClick = null,
        )
    }

    private fun toggleWalletExpanded(walletId: UserWalletId) {
        expandedWallets.update { current ->
            if (walletId in current) current - walletId else current + walletId
        }
    }

    private fun createSearchBar(query: String): SearchBarUM {
        return SearchBarUM(
            placeholderText = resourceReference(com.tangem.core.ui.R.string.common_search_tokens),
            query = query,
            onQueryChange = { searchQuery.value = it },
            isActive = query.isNotEmpty(),
            onActiveChange = {},
        )
    }

    private fun buildInitialState(): NetworkSelectionUM {
        return NetworkSelectionUM(
            searchBar = createSearchBar(""),
            walletGroups = persistentListOf(),
            isBalanceHidden = true,
        )
    }

    private data class TokenMappingParams(
        val userWalletId: UserWalletId,
        val appCurrency: AppCurrency,
        val statusMap: Map<CryptoCurrency.ID, CryptoCurrencyStatus>,
    )
}