package com.tangem.features.commonfeatures.api.choosetoken

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.commonfeatures.api.R
import com.tangem.features.commonfeatures.api.choosetoken.model.ChooseTokenPortfolioFullBlockUM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ChooseTokenBridge : ChooseTokenBridgeInternal {

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
        val isAppBarShown: Boolean = true,
        /**
         * When `true`, single-currency wallets (and single-currency-with-token wallets like NODL)
         * are shown as selectable tabs. Swap flows keep this `false` — only multi-currency wallets apply there.
         */
        val isShowSingleCurrencyWallets: Boolean = false,
    ) {
        companion object {
            val SwapFrom = Settings(
                title = resourceReference(R.string.swapping_from_title),
                isShowMarketBlock = true,
                isShowPaymentAccount = true,
            )
            val SwapTo = Settings(
                title = resourceReference(R.string.swapping_to_title),
                isShowMarketBlock = true,
                isShowPaymentAccount = true,
            )
            val AddFunds = Settings(
                title = resourceReference(R.string.swapping_to_title),
                isShowMarketBlock = true,
                isShowPaymentAccount = false,
                isAppBarShown = false,
                isShowSingleCurrencyWallets = true,
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

    /** Currently selected wallet tab. Used to constrain feature blocks (e.g. market block) to the wallet's type. */
    val selectedWalletFlow: SharedFlow<UserWallet>

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

data class ChooseTokenResult(
    val currency: CryptoCurrencyStatus,
    val account: AccountStatus,
    val wallet: UserWallet,
    val analyticsPayload: Set<ChooseTokenAnalyticsPayload> = emptySet(),
) {
    val walletId get() = wallet.walletId

    val wasJustAdded: Boolean
        get() = analyticsPayload
            .filterIsInstance<ChooseTokenAnalyticsPayload.IsMarketTokenSelected>()
            .any { it.value }
}

sealed interface ChooseTokenAnalyticsPayload {

    @Suppress("BooleanPropertyNaming")
    @JvmInline
    value class IsSearched(val value: Boolean) : ChooseTokenAnalyticsPayload

    @JvmInline
    value class ScreensSources(val value: String) : ChooseTokenAnalyticsPayload

    @Suppress("BooleanPropertyNaming")
    @JvmInline
    value class IsMarketTokenSelected(val value: Boolean) : ChooseTokenAnalyticsPayload
}