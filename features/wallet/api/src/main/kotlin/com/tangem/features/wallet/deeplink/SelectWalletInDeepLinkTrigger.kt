package com.tangem.features.wallet.deeplink

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface WalletDeepLinkActionTrigger {
    fun selectWallet(userWalletId: UserWalletId)
    fun showTangemPayTransaction(transaction: TangemPayTxHistoryItem, customerId: String)

    fun onOpenedByLinkOrPush()
}

interface WalletDeepLinkActionListener {
    val selectWalletFlow: Flow<UserWalletId>
    val showTangemPayTransactionFlow: Flow<TangemPayTransactionDeepLinkData>

    /** Whether a deep link or a push notification has opened the app in this process. */
    val isOpenedByLinkOrPush: StateFlow<Boolean>
}

data class TangemPayTransactionDeepLinkData(
    val transaction: TangemPayTxHistoryItem,
    val customerId: String,
)