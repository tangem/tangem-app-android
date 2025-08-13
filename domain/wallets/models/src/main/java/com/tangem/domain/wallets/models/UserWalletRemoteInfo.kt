package com.tangem.domain.wallets.models

import com.tangem.domain.models.wallet.UserWalletId

class UserWalletRemoteInfo(
    val walletId: UserWalletId,
    val name: String,
    val isNotificationsEnabled: Boolean,
)