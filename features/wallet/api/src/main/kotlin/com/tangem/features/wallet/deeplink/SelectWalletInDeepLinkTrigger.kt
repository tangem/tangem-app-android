package com.tangem.features.wallet.deeplink

import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface WalletDeepLinkActionTrigger {
    fun selectWallet(userWalletId: UserWalletId)
}

interface WalletDeepLinkActionListener {
    val selectWalletFlow: Flow<UserWalletId>
}