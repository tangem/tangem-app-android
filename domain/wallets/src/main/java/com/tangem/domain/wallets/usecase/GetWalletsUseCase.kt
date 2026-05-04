package com.tangem.domain.wallets.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.LinkedHashMap

/**
 * Use case for getting list of user wallets
 *
 * @property userWalletsListRepository repository for getting list of user wallets
 *
[REDACTED_AUTHOR]
 */
class GetWalletsUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    @Throws(IllegalArgumentException::class)
    operator fun invoke(): Flow<List<UserWallet>> = userWalletsListRepository.userWallets.map { requireNotNull(it) }

    @Throws(IllegalArgumentException::class)
    fun invokeAsMap(): Flow<LinkedHashMap<UserWalletId, UserWallet>> = userWalletsListRepository.userWallets
        .map { requireNotNull(it) }
        .map { wallets ->
            wallets.associateByTo(
                destination = linkedMapOf(),
                keySelector = { wallet -> wallet.walletId },
            )
        }

    @Throws(IllegalArgumentException::class)
    fun invokeSync(): List<UserWallet> = userWalletsListRepository.userWallets.value!!
}