package com.tangem.domain.wallets.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.utils.coroutines.runSuspendCatching
import timber.log.Timber

/**
 * Use case to sync wallet with remote
 *
[REDACTED_AUTHOR]
 */
class SyncWalletWithRemoteUseCase(
    private val walletsRepository: WalletsRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId) {
        runSuspendCatching { walletsRepository.createWallet(userWalletId) }
            .onFailure(Timber::e)
    }
}