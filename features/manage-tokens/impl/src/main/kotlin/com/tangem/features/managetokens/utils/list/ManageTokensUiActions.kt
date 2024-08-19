package com.tangem.features.managetokens.utils.list

import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

internal interface ManageTokensUiActions {

    fun addCurrency(batchKey: Int, currencyId: ManagedCryptoCurrency.ID, networkId: Network.ID)

    fun removeCurrency(batchKey: Int, currencyId: ManagedCryptoCurrency.ID, networkId: Network.ID)

    fun checkNeedToShowRemoveNetworkWarning(currencyId: ManagedCryptoCurrency.ID, networkId: Network.ID): Boolean

    suspend fun checkHasLinkedTokens(userWalletId: UserWalletId, network: Network): Boolean
}