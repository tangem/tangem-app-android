package com.tangem.domain.wallets.legacy

import kotlinx.coroutines.flow.Flow

interface WalletsStateHolder {

    val userWalletsListManager: UserWalletsListManager?

    val userWalletListManagerFlow: Flow<UserWalletsListManager?>
}