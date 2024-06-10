package com.tangem.domain.wallets.usecase

import com.tangem.domain.wallets.legacy.UserWalletsListManager

/**
 * Use case for getting list of user wallets names.
 *
 * @property userWalletsListManager user wallets list manager
 */
class GetWalletNamesUseCase(private val userWalletsListManager: UserWalletsListManager) {

    operator fun invoke(): List<String> = userWalletsListManager.userWalletsSync.map { it.name }
}