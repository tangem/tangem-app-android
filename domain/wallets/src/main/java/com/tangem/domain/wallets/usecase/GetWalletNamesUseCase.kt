package com.tangem.domain.wallets.usecase

import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.core.wallets.requireUserWalletsSync

/**
 * Use case for getting list of user wallets names.
 *
 * @property userWalletsListManager user wallets list manager
 */
class GetWalletNamesUseCase(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val useNewRepository: Boolean,
) {

    operator fun invoke(): List<String> = if (useNewRepository) {
        userWalletsListRepository.requireUserWalletsSync().map { it.name }
    } else {
        userWalletsListManager.userWalletsSync.map { it.name }
    }
}