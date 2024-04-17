package com.tangem.domain.wallets.legacy

import kotlinx.coroutines.flow.Flow

// TODO: will be remove in this task [REDACTED_JIRA]
@Deprecated(message = "Provide UserWalletsListManager using DI", level = DeprecationLevel.WARNING)
interface WalletsStateHolder {

    val userWalletsListManager: UserWalletsListManager?

    val userWalletListManagerFlow: Flow<UserWalletsListManager?>
}