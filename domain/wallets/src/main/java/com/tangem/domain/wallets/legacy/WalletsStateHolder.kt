package com.tangem.domain.wallets.legacy

import kotlinx.coroutines.flow.Flow
// [REDACTED_TODO_COMMENT]
@Deprecated(message = "Provide UserWalletsListManager using DI", level = DeprecationLevel.WARNING)
interface WalletsStateHolder {

    val userWalletsListManager: UserWalletsListManager?

    val userWalletListManagerFlow: Flow<UserWalletsListManager?>
}
