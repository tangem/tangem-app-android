package com.tangem.domain.wallets.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
    fun invokeAsMap(isOnlyMultiCurrency: Boolean = true): Flow<LinkedHashMap<UserWalletId, UserWallet>> = invoke()
        .map { list ->
            val wallets = if (isOnlyMultiCurrency) {
                list.filter { wallet -> wallet.isMultiCurrency }
            } else {
                list
            }
            wallets.associateByTo(
                destination = linkedMapOf(),
                keySelector = { wallet -> wallet.walletId },
            )
        }

    @Throws(IllegalArgumentException::class)
    fun invokeSync(): List<UserWallet> = userWalletsListRepository.userWallets.value!!
}