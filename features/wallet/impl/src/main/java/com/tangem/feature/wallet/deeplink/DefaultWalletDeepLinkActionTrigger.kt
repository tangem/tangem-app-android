package com.tangem.feature.wallet.deeplink

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.wallet.deeplink.TangemPayTransactionDeepLinkData
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionListener
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionTrigger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultWalletDeepLinkActionTrigger @Inject constructor() :
    WalletDeepLinkActionTrigger,
    WalletDeepLinkActionListener {

    private val _selectWalletFlow = Channel<UserWalletId>()
    override val selectWalletFlow: Flow<UserWalletId>
        get() = _selectWalletFlow.receiveAsFlow()

    private val _showTangemPayTransactionFlow = Channel<TangemPayTransactionDeepLinkData>()
    override val showTangemPayTransactionFlow: Flow<TangemPayTransactionDeepLinkData>
        get() = _showTangemPayTransactionFlow.receiveAsFlow()

    override fun selectWallet(userWalletId: UserWalletId) {
        _selectWalletFlow.trySend(userWalletId)
    }

    override fun showTangemPayTransaction(transaction: TangemPayTxHistoryItem, customerId: String) {
        _showTangemPayTransactionFlow.trySend(TangemPayTransactionDeepLinkData(transaction, customerId))
    }
}