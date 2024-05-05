package com.tangem.domain.wallets.usecase

import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.flow.Flow

/**
 * Use case for getting list of user wallets
 *
 * @property userWalletsListManager user wallets list manager
 *
* [REDACTED_AUTHOR]
 */
class GetWalletsUseCase(private val userWalletsListManager: UserWalletsListManager) {

    @Throws(IllegalArgumentException::class)
    operator fun invoke(): Flow<List<UserWallet>> = userWalletsListManager.userWallets
}
