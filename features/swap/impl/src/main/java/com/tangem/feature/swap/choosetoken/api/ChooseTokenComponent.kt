package com.tangem.feature.swap.choosetoken.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.swap.domain.models.ui.CurrenciesGroup
import com.tangem.feature.swap.presentation.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// todo swap make universal, encapsulate, move to some common module
internal interface ChooseTokenBridge {

    // todo swap new api
    val onCurrencyChosen: Channel<ChooseTokenResult>
    val onClose: Channel<Unit>

    // todo swap legacy api, remove
    val onTokenSelected: Channel<Pair<String, ChooseTokenAnalyticsPayload.IsSearched>>
    val onNewTokenAdded: Channel<Pair<CryptoCurrency, ChooseTokenAnalyticsPayload.IsSearched>>

    val searchQueryState: StateFlow<String>
    val currenciesGroup: Flow<CurrenciesGroup>

    fun onTokenSelected(tokenId: Pair<String, ChooseTokenAnalyticsPayload.IsSearched>) {
        onTokenSelected.trySend(tokenId)
        onSearchQuery("")
    }

    fun onNewTokenAdded(addedToken: Pair<CryptoCurrency, ChooseTokenAnalyticsPayload.IsSearched>) {
        onNewTokenAdded.trySend(addedToken)
        onSearchQuery("")
    }

    fun onSearchQuery(query: String)

    fun updateCurrenciesGroup(currenciesGroup: CurrenciesGroup)

    fun onCurrencyChosen(result: ChooseTokenResult) {
        onCurrencyChosen.trySend(result)
    }

    fun onClose() {
        onClose.trySend(Unit)
        onSearchQuery("")
    }

    interface Factory {
        fun create(modelScope: CoroutineScope): ChooseTokenBridge
    }
}

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

internal interface ChooseTokenComponent : ComposableContentComponent {

    data class Params(
        val bridge: ChooseTokenBridge,
        val settings: Settings,
        val analyticsPayload: Set<ChooseTokenAnalyticsPayload> = emptySet(),
    )

    data class Settings(
        val title: TextReference,
        val isShowMarketBlock: Boolean,
    ) {
        companion object {
            val SwapFrom = Settings(
                title = resourceReference(R.string.swapping_from_title),
                isShowMarketBlock = false,
            )
            val SwapTo = Settings(
                title = resourceReference(R.string.swapping_to_title),
                isShowMarketBlock = true,
            )
        }
    }

    interface Factory : ComponentFactory<Params, ChooseTokenComponent>
}