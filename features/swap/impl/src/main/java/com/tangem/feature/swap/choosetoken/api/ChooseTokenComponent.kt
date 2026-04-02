package com.tangem.feature.swap.choosetoken.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.swap.domain.models.ui.CurrenciesGroup
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
    val addedCurrency: AccountCryptoCurrencyStatus,
    val userWallet: UserWallet,
    val analyticsPayload: Set<ChooseTokenAnalyticsPayload> = emptySet(),
) {
    val status: CryptoCurrencyStatus get() = addedCurrency.status
    val account: Account.CryptoPortfolio get() = addedCurrency.account
    val walletId get() = userWallet.walletId
}

sealed interface ChooseTokenAnalyticsPayload {

    @Suppress("BooleanPropertyNaming")
    @JvmInline
    value class IsSearched(val value: Boolean) : ChooseTokenAnalyticsPayload
}

internal interface ChooseTokenComponent : ComposableContentComponent {

    data class Params(
        val bridge: ChooseTokenBridge,
    )

    interface Factory : ComponentFactory<Params, ChooseTokenComponent>
}