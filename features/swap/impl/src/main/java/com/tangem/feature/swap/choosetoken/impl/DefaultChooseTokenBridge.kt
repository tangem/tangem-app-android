package com.tangem.feature.swap.choosetoken.impl

import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.swap.choosetoken.api.ChooseTokenAnalyticsPayload
import com.tangem.feature.swap.choosetoken.api.ChooseTokenBridge
import com.tangem.feature.swap.choosetoken.api.ChooseTokenBridge.Settings
import com.tangem.feature.swap.choosetoken.api.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.feature.swap.choosetoken.api.ChooseTokenResult
import com.tangem.feature.swap.choosetoken.api.ChooseTokenResultOld
import com.tangem.feature.swap.choosetoken.api.model.ChooseTokenPortfolioFullBlockUM
import com.tangem.feature.swap.choosetoken.impl.model.ChooseTokenModel.Companion.DEBOUNCE_SEARCH_DELAY
import com.tangem.feature.swap.choosetoken.impl.model.PortfolioFullBlockDelegate
import com.tangem.feature.swap.choosetoken.impl.model.PortfolioListBlockDelegate
import com.tangem.feature.swap.domain.models.ui.CurrenciesGroup
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

internal class DefaultChooseTokenBridge @AssistedInject constructor(
    @Assisted private val modelScope: CoroutineScope,
    portfolioListBlockDelegateFactory: PortfolioListBlockDelegate.Factory,
    portfolioFullBlockDelegateFactory: PortfolioFullBlockDelegate.Factory,
    @Assisted override val settings: Settings,
    @Assisted override val analyticsPayload: Set<ChooseTokenAnalyticsPayload>,
) : ChooseTokenBridge {

    override val onCurrencyChosen: Channel<ChooseTokenResult> = Channel()

    override val onTokenSelected: Channel<ChooseTokenResultOld> = Channel()
    override val onNewTokenAdded: Channel<Pair<CryptoCurrency, ChooseTokenAnalyticsPayload.IsSearched>> = Channel()
    override val onClose: Channel<Unit> = Channel()

    private val onSearchQuery: Channel<SearchQuery> = Channel()
    override val searchQueryState: StateFlow<SearchQuery> = onSearchQuery.receiveAsFlow()
        .debounce(DEBOUNCE_SEARCH_DELAY)
        .stateIn(modelScope, SharingStarted.Eagerly, initialValue = SearchQuery.Empty)

    private val portfolioListBlockDelegate: PortfolioListBlockDelegate = portfolioListBlockDelegateFactory.create(
        modelScope = modelScope,
        searchQueryState = searchQueryState,
        featureSettings = settings,
    )

    private val portfolioFullBlockDelegate: PortfolioFullBlockDelegate = portfolioFullBlockDelegateFactory.create(
        modelScope = modelScope,
        searchQueryState = searchQueryState,
        portfolioListBlockDelegate = portfolioListBlockDelegate,
    )

    override val tokenFilter: MutableStateFlow<(AccountStatus, CryptoCurrencyStatus) -> Boolean>
        get() = portfolioListBlockDelegate.tokenFilter

    override val fullPortfolioBlock: StateFlow<ChooseTokenPortfolioFullBlockUM?>
        get() = portfolioFullBlockDelegate.fullPortfolioBlock

    private val _currenciesGroupFlow = MutableStateFlow<CurrenciesGroup?>(null)
    override val currenciesGroup: Flow<CurrenciesGroup> = _currenciesGroupFlow.filterNotNull()

    init {
        portfolioListBlockDelegate.onTokenChosen.receiveAsFlow()
            .onEach { chooseResult -> onCurrencyChosen(chooseResult) }
            .launchIn(modelScope)
    }

    override fun selectWalletTab(walletId: UserWalletId) {
        portfolioFullBlockDelegate.selectWalletTab(walletId)
    }

    override fun onSearchQuery(query: SearchQuery) {
        onSearchQuery.trySend(query)
    }

    override fun onCurrencyChosen(result: ChooseTokenResult) {
        onCurrencyChosen.trySend(result)
        onSearchQuery(SearchQuery.Empty)
    }

    override fun onClose() {
        onClose.trySend(Unit)
        onSearchQuery(SearchQuery.Empty)
    }

    override fun updateCurrenciesGroup(currenciesGroup: CurrenciesGroup) {
        _currenciesGroupFlow.update { currenciesGroup }
    }

    @AssistedFactory
    interface Factory : ChooseTokenBridge.Factory {
        override fun create(
            modelScope: CoroutineScope,
            settings: Settings,
            analyticsPayload: Set<ChooseTokenAnalyticsPayload>,
        ): DefaultChooseTokenBridge
    }
}