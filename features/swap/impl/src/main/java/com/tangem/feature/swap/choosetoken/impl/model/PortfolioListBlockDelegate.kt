package com.tangem.feature.swap.choosetoken.impl.model

import com.tangem.common.ui.tokens.TokenConverterParams
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.account.status.utils.ExpandedAccountsHolder
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.feature.swap.choosetoken.api.SettingContextUseCase
import com.tangem.feature.swap.choosetoken.impl.converter.ChooseTokenListItemConverter
import com.tangem.feature.swap.models.TokenListUMData
import com.tangem.utils.extensions.mapNotNullValues
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

internal class PortfolioListBlockDelegate @AssistedInject constructor(
    private val expandedAccountsHolder: ExpandedAccountsHolder,
    private val settingContext: SettingContextUseCase,
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    private val getWalletsUseCase: GetWalletsUseCase,
    @Assisted private val modelScope: CoroutineScope,
    @Assisted private val searchQueryState: StateFlow<String>,
) : ClickIntents {

    val onTokenItemClick: Channel<Pair<AccountStatus, CryptoCurrencyStatus>> = Channel()

    val portfolioList: Flow<Map<UserWalletId, TokenListUMData>> = flow {
        val allAccountsFlow: Flow<LinkedHashMap<UserWalletId, AccountStatusList>> =
            multiAccountStatusListSupplier.invokeAsMap()

        val allWalletsFlow: Flow<LinkedHashMap<UserWalletId, UserWallet>> =
            getWalletsUseCase.invokeAsMap()

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
            transform = { settings, allAccounts, expandedAccountsMap, searchQuery ->
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
                    ).convert()

                    um
                }
            },
        )
        emitAll(finalFlow)
    }
        .distinctUntilChanged()
        .shareIn(modelScope, SharingStarted.Eagerly, replay = 1)

    private fun List<UserWalletId>.toExpandedAccountsMap(): Flow<Map<UserWalletId, Set<AccountId>>> {
        if (isEmpty()) return flowOf(emptyMap())
        val flows: List<Flow<Pair<UserWalletId, Set<AccountId>>>> =
            map { walletId -> expandedAccountsHolder.expandedAccounts(walletId).map { set -> walletId to set } }
        return combine(flows, { pairs -> pairs.toMap() })
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
        fun create(searchQueryState: StateFlow<String>, modelScope: CoroutineScope): PortfolioListBlockDelegate
    }
}

internal interface ClickIntents {
    fun onTokenItemClick(account: AccountStatus, currencyStatus: CryptoCurrencyStatus)

    fun onAccountExpandClick(account: Account)

    fun onAccountCollapseClick(account: Account)
}