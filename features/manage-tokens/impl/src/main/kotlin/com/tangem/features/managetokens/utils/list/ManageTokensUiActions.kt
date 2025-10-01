package com.tangem.features.managetokens.utils.list

import com.tangem.domain.managetokens.model.CurrencyUnsupportedState
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.models.network.Network

internal interface ManageTokensUiActions {

    fun onTokenClick(currency: ManagedCryptoCurrency.Token)

    fun addCurrency(batchKey: Int, currency: ManagedCryptoCurrency.Token, network: Network)

    fun removeCurrency(batchKey: Int, currency: ManagedCryptoCurrency.Token, network: Network)

    fun removeCustomCurrency(currency: ManagedCryptoCurrency.Custom)

    fun checkNeedToShowRemoveNetworkWarning(currency: ManagedCryptoCurrency.Token, network: Network): Boolean

    suspend fun checkHasLinkedTokens(network: Network): Boolean

    suspend fun checkCurrencyUnsupportedState(
        sourceNetwork: ManagedCryptoCurrency.SourceNetwork,
    ): CurrencyUnsupportedState?
}