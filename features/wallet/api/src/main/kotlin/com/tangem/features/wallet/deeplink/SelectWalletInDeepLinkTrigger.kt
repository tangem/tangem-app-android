package com.tangem.features.wallet.deeplink

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import kotlinx.coroutines.flow.Flow

interface WalletDeepLinkActionTrigger {
    fun selectWallet(userWalletId: UserWalletId)
    fun showTangemPayTransaction(transaction: TangemPayTxHistoryItem, customerId: String)
}

interface WalletDeepLinkActionListener {
    val selectWalletFlow: Flow<UserWalletId>
    val showTangemPayTransactionFlow: Flow<TangemPayTransactionDeepLinkData>
}

data class TangemPayTransactionDeepLinkData(
    val transaction: TangemPayTxHistoryItem,
    val customerId: String,
)