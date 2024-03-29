package com.tangem.domain.wallets.usecase

import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.flow.Flow

/**
 * Use case for getting list of user wallets
 *
 * @property walletsStateHolder state holder for getting static initialized 'userWalletsListManager'
 *
 * @author Andrew Khokhlov on 07/07/2023
 */
class GetWalletsUseCase(private val walletsStateHolder: WalletsStateHolder) {

    @Throws(IllegalArgumentException::class)
    operator fun invoke(): Flow<List<UserWallet>> {
        return requireNotNull(walletsStateHolder.userWalletsListManager).userWallets
    }
}
