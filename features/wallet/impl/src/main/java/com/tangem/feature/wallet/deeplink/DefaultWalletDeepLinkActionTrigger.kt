package com.tangem.feature.wallet.deeplink

import com.tangem.domain.wallets.models.UserWalletId
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

    override fun selectWallet(userWalletId: UserWalletId) {
        _selectWalletFlow.trySend(userWalletId)
    }
}