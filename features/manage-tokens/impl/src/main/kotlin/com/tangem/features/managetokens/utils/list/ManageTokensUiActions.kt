package com.tangem.features.managetokens.utils.list

import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

internal interface ManageTokensUiActions {

    fun addCurrency(batchKey: Int, currency: ManagedCryptoCurrency.Token, network: Network)

    fun removeCurrency(batchKey: Int, currency: ManagedCryptoCurrency.Token, network: Network)

    fun checkNeedToShowRemoveNetworkWarning(currency: ManagedCryptoCurrency.Token, network: Network): Boolean

    suspend fun checkHasLinkedTokens(userWalletId: UserWalletId, network: Network): Boolean
}