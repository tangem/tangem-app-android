package com.tangem.domain.wallets.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
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

    /**
     * A null list means the wallets are not loaded yet, which happens on every cold start and on a
     * process restore, where a screen may be rebuilt before the load finishes. The flow stays silent
     * until the list arrives instead of failing the collector.
     */
    operator fun invoke(): Flow<List<UserWallet>> = userWalletsListRepository.userWallets.filterNotNull()

    fun invokeAsMap(
        isOnlyMultiCurrency: Boolean = true,
        filterLocked: Boolean = false,
    ): Flow<LinkedHashMap<UserWalletId, UserWallet>> = invoke()
        .map { list ->
            val wallets = list
                .filter { wallet -> if (isOnlyMultiCurrency) wallet.isMultiCurrency else true }
                .filter { wallet -> if (filterLocked) !wallet.isLocked else true }
            wallets.associateByTo(
                destination = linkedMapOf(),
                keySelector = { wallet -> wallet.walletId },
            )
        }

    @Throws(IllegalArgumentException::class)
    fun invokeSync(): List<UserWallet> = requireNotNull(userWalletsListRepository.userWallets.value) {
        "User wallets are not loaded"
    }
}