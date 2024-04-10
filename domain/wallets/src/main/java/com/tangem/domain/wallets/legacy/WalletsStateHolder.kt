package com.tangem.domain.wallets.legacy

import kotlinx.coroutines.flow.Flow

// TODO: will be remove in this task https://tangem.atlassian.net/browse/AND-6714
@Deprecated(message = "Provide UserWalletsListManager using DI", level = DeprecationLevel.WARNING)
interface WalletsStateHolder {

    val userWalletsListManager: UserWalletsListManager?

    val userWalletListManagerFlow: Flow<UserWalletsListManager?>
}
