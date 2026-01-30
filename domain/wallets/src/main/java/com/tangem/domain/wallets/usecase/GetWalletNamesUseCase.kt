package com.tangem.domain.wallets.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.requireUserWalletsSync

/**
 * Use case for getting list of user wallets names.
 *
 * @property userWalletsListRepository repository for getting list of user wallets
 */
class GetWalletNamesUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    operator fun invoke(): List<String> = userWalletsListRepository.requireUserWalletsSync().map { it.name }
}