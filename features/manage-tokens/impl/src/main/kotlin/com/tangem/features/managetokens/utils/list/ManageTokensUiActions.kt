package com.tangem.features.managetokens.utils.list

import com.tangem.domain.managetokens.model.CurrencyUnsupportedState
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.UserWalletId

internal interface ManageTokensUiActions {

    fun addCurrency(batchKey: Int, currency: ManagedCryptoCurrency.Token, network: Network)

    fun removeCurrency(batchKey: Int, currency: ManagedCryptoCurrency.Token, network: Network)

    fun removeCustomCurrency(userWalletId: UserWalletId, currency: ManagedCryptoCurrency.Custom)

    fun checkNeedToShowRemoveNetworkWarning(currency: ManagedCryptoCurrency.Token, network: Network): Boolean

    suspend fun checkHasLinkedTokens(userWalletId: UserWalletId, network: Network): Boolean

    suspend fun checkCurrencyUnsupportedState(
        userWalletId: UserWalletId,
        sourceNetwork: ManagedCryptoCurrency.SourceNetwork,
    ): CurrencyUnsupportedState?
}