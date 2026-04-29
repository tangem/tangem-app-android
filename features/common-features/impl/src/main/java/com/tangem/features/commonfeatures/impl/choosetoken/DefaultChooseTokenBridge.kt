package com.tangem.features.commonfeatures.impl.choosetoken

import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenAnalyticsPayload
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge.Settings
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenResult
import com.tangem.features.commonfeatures.api.choosetoken.model.ChooseTokenPortfolioFullBlockUM
import com.tangem.features.commonfeatures.impl.choosetoken.model.PortfolioFullBlockDelegate
import com.tangem.features.commonfeatures.impl.choosetoken.model.PortfolioListBlockDelegate
import com.tangem.features.commonfeatures.impl.choosetoken.model.ChooseTokenModel
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
    override val onClose: Channel<Unit> = Channel()

    private val onSearchQuery: Channel<SearchQuery> = Channel()
    override val searchQueryState: StateFlow<SearchQuery> = onSearchQuery.receiveAsFlow()
        .debounce(ChooseTokenModel.Companion.DEBOUNCE_SEARCH_DELAY)
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

    @AssistedFactory
    interface Factory : ChooseTokenBridge.Factory {
        override fun create(
            modelScope: CoroutineScope,
            settings: Settings,
            analyticsPayload: Set<ChooseTokenAnalyticsPayload>,
        ): DefaultChooseTokenBridge
    }
}