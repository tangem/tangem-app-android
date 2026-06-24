package com.tangem.features.commonfeatures.impl.choosetoken.model

import com.tangem.common.ui.tokens.TokenConverterParams
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.account.status.utils.ChooseTokenExpandedAccountsHolder
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenAnalyticsPayload
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery.Companion.isSearchingState
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenResult
import com.tangem.features.commonfeatures.api.choosetoken.model.TokenListUMData
import com.tangem.features.commonfeatures.impl.choosetoken.SettingContextUseCase
import com.tangem.features.commonfeatures.impl.choosetoken.converter.ChooseTokenListItemConverter
import com.tangem.utils.extensions.mapNotNullValues
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

@Suppress("LongParameterList")
internal class PortfolioListBlockDelegate @AssistedInject constructor(
    private val expandedAccountsHolder: ChooseTokenExpandedAccountsHolder,
    private val settingContext: SettingContextUseCase,
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    private val getWalletsUseCase: GetWalletsUseCase,
    @Assisted private val modelScope: CoroutineScope,
    @Assisted private val searchQueryState: StateFlow<SearchQuery>,
    @Assisted private val featureSettings: ChooseTokenBridge.Settings,
) : ClickIntents {

    private val onTokenItemClick: Channel<Pair<AccountStatus, CryptoCurrencyStatus>> = Channel()

    private val isOnlyMultiCurrency: Boolean get() = !featureSettings.isShowSingleCurrencyWallets

    val onTokenChosen: Channel<ChooseTokenResult> = Channel()
    val tokenFilter: MutableStateFlow<(AccountStatus, CryptoCurrencyStatus) -> Boolean> =
        MutableStateFlow { _, _ -> true }

    val portfolioList: SharedFlow<Map<UserWalletId, TokenListUMData>> = buildDataFlow()
        .distinctUntilChanged()
        .shareIn(modelScope, SharingStarted.Eagerly, replay = 1)

    private fun buildDataFlow(): Flow<Map<UserWalletId, TokenListUMData>> = channelFlow {
        val allAccountsFlow: Flow<LinkedHashMap<UserWalletId, AccountStatusList>> =
            multiAccountStatusListSupplier.invokeAsMap()

        val allWalletsFlow: StateFlow<LinkedHashMap<UserWalletId, UserWallet>> =
            getWalletsUseCase.invokeAsMap(isOnlyMultiCurrency = isOnlyMultiCurrency).stateIn(this)

        onTokenItemClick.receiveAsFlow()
            .onEach { (account, currencyStatus) ->
                onTokenItemClick(
                    wallet = allWalletsFlow.value[account.accountId.userWalletId] ?: return@onEach,
                    account = account,
                    currencyStatus = currencyStatus,
                )
            }
            .launchIn(this)

        val expandedAccountsMapFlow = allWalletsFlow
            .map { allWallets -> allWallets.values.map { wallet -> wallet.walletId } }
            .distinctUntilChanged()
            .flatMapLatest { walletIds -> walletIds.toExpandedAccountsMap() }
            .distinctUntilChanged()

        val finalFlow = combine(
            flow = settingContext.invoke(),
            flow2 = allAccountsFlow,
            flow3 = expandedAccountsMapFlow,
            flow4 = searchQueryState,
            flow5 = tokenFilter,
            transform = { settings, allAccounts, expandedAccountsMap, searchQuery, tokenFilter ->
                allAccounts.mapNotNullValues { (walletId, statusList) ->
                    val expandedAccounts = expandedAccountsMap[walletId].orEmpty()
                    val converterParams = if (settings.isAccountsMode) {
                        TokenConverterParams.Account(statusList, expandedAccounts)
                    } else {
                        TokenConverterParams.Wallet(statusList.mainAccount, statusList.mainAccount.tokenList)
                    }

                    val um = ChooseTokenListItemConverter(
                        appCurrency = settings.appCurrency,
                        params = converterParams,
                        clickIntents = this@PortfolioListBlockDelegate,
                        searchQuery = searchQuery,
                        tokenFilter = tokenFilter,
                        isShowPaymentAccount = featureSettings.isShowPaymentAccount,
                    ).convert()

                    um
                }
            },
        )
        finalFlow.collectLatest { result -> channel.send(result) }
    }

    private fun List<UserWalletId>.toExpandedAccountsMap(): Flow<Map<UserWalletId, Set<AccountId>>> {
        if (isEmpty()) return flowOf(emptyMap())
        val flows: List<Flow<Pair<UserWalletId, Set<AccountId>>>> =
            map { walletId -> expandedAccountsHolder.expandedAccounts(walletId).map { set -> walletId to set } }
        return combine(flows, { pairs -> pairs.toMap() })
    }

    private fun onTokenItemClick(wallet: UserWallet, account: AccountStatus, currencyStatus: CryptoCurrencyStatus) {
        val analyticsPayload = setOf(
            ChooseTokenAnalyticsPayload.IsSearched(searchQueryState.isSearchingState),
            ChooseTokenAnalyticsPayload.IsMarketTokenSelected(false),
        )
        val result = ChooseTokenResult(
            account = account,
            currency = currencyStatus,
            wallet = wallet,
            analyticsPayload = analyticsPayload,
        )
        onTokenChosen.trySend(result)
    }

    override fun onTokenItemClick(account: AccountStatus, currencyStatus: CryptoCurrencyStatus) {
        onTokenItemClick.trySend(account to currencyStatus)
    }

    override fun onAccountExpandClick(account: Account) {
        expandedAccountsHolder.expandAccount(account.accountId)
    }

    override fun onAccountCollapseClick(account: Account) {
        expandedAccountsHolder.collapseAccount(account.accountId)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            searchQueryState: StateFlow<SearchQuery>,
            modelScope: CoroutineScope,
            featureSettings: ChooseTokenBridge.Settings,
        ): PortfolioListBlockDelegate
    }
}

internal interface ClickIntents {
    fun onTokenItemClick(account: AccountStatus, currencyStatus: CryptoCurrencyStatus)

    fun onAccountExpandClick(account: Account)

    fun onAccountCollapseClick(account: Account)
}