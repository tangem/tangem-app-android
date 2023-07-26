package com.tangem.datasource.local.walletmanager.model

import com.tangem.blockchain.common.WalletManager
import com.tangem.domain.wallets.models.UserWalletId

internal data class StoredWalletManagers(
    val userWalletId: UserWalletId,
    val walletManagers: List<WalletManager>,
)
