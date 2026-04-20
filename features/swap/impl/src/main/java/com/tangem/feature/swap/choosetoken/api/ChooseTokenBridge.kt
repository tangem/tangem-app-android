package com.tangem.feature.swap.choosetoken.api

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.swap.choosetoken.api.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.feature.swap.choosetoken.api.model.ChooseTokenPortfolioFullBlockUM
import com.tangem.feature.swap.domain.models.ui.CurrenciesGroup
import com.tangem.feature.swap.presentation.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// todo swap move to common-features module
interface ChooseTokenBridge : ChooseTokenBridgeLegacy, ChooseTokenBridgeInternal {

    val onCurrencyChosen: Channel<ChooseTokenResult>
    val onClose: Channel<Unit>

    /**
     * for some Feature specific tokens filtering
     */
    val tokenFilter: MutableStateFlow<(AccountStatus, CryptoCurrencyStatus) -> Boolean>

    fun selectWalletTab(walletId: UserWalletId)

    data class Settings(
        val title: TextReference,
        val isShowMarketBlock: Boolean,
        val isShowPaymentAccount: Boolean,
    ) {
        companion object {
            val SwapFrom = Settings(
                title = resourceReference(R.string.swapping_from_title),
                isShowMarketBlock = false,
                isShowPaymentAccount = true,
            )
            val SwapTo = Settings(
                title = resourceReference(R.string.swapping_to_title),
                isShowMarketBlock = true,
                isShowPaymentAccount = true,
            )
        }
    }

    interface Factory {
        fun create(
            modelScope: CoroutineScope,
            settings: Settings,
            analyticsPayload: Set<ChooseTokenAnalyticsPayload> = emptySet(),
        ): ChooseTokenBridge
    }
}

/**
 * primary for internal impl usage, but you can also use it externally
 */
interface ChooseTokenBridgeInternal {
    val settings: ChooseTokenBridge.Settings
    val analyticsPayload: Set<ChooseTokenAnalyticsPayload>
    val searchQueryState: StateFlow<SearchQuery>
    val fullPortfolioBlock: StateFlow<ChooseTokenPortfolioFullBlockUM?>

    fun onSearchQuery(query: SearchQuery)
    fun onSearchQuery(query: String) = onSearchQuery(SearchQuery(query))

    fun onClose()
    fun onCurrencyChosen(result: ChooseTokenResult)

    @JvmInline
    value class SearchQuery(val value: String) {
        companion object {
            val Empty = SearchQuery("")
            val SearchQuery.isSearchingState: Boolean get() = this.value.isNotBlank()
            val StateFlow<SearchQuery>.isSearchingState: Boolean get() = this.value.isSearchingState
        }
    }
}

// todo swap legacy api, remove
interface ChooseTokenBridgeLegacy : ChooseTokenBridgeInternal {

    val onTokenSelected: Channel<ChooseTokenResultOld>
    val onNewTokenAdded: Channel<Pair<CryptoCurrency, ChooseTokenAnalyticsPayload.IsSearched>>

    val currenciesGroup: Flow<CurrenciesGroup>

    fun onTokenSelected(result: ChooseTokenResultOld) {
        onTokenSelected.trySend(result)
        onSearchQuery(SearchQuery.Empty)
    }

    fun onNewTokenAdded(addedToken: Pair<CryptoCurrency, ChooseTokenAnalyticsPayload.IsSearched>) {
        onNewTokenAdded.trySend(addedToken)
        onSearchQuery(SearchQuery.Empty)
    }

    fun updateCurrenciesGroup(currenciesGroup: CurrenciesGroup)
}

data class ChooseTokenResultOld(
    val cryptoCurrencyStatus: CryptoCurrencyStatus,
    val account: Account,
    val isSearched: Boolean,
)

data class ChooseTokenResult(
    val currency: CryptoCurrencyStatus,
    val account: AccountStatus,
    val wallet: UserWallet,
    val analyticsPayload: Set<ChooseTokenAnalyticsPayload> = emptySet(),
) {
    val walletId get() = wallet.walletId
}

sealed interface ChooseTokenAnalyticsPayload {

    @Suppress("BooleanPropertyNaming")
    @JvmInline
    value class IsSearched(val value: Boolean) : ChooseTokenAnalyticsPayload

    @JvmInline
    value class ScreensSources(val value: String) : ChooseTokenAnalyticsPayload
}