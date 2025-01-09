package com.tangem.domain.wallets.usecase

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository
import kotlinx.coroutines.flow.Flow

class SeedPhraseNotificationUseCase(
    private val walletsRepository: WalletsRepository,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<Boolean> {
        return walletsRepository.seedPhraseNotificationStatus(userWalletId)
    }

    suspend fun notified(userWalletId: UserWalletId) {
        walletsRepository.notifiedSeedPhraseNotification(userWalletId)
    }

    suspend fun confirm(userWalletId: UserWalletId) {
        walletsRepository.confirmSeedPhraseNotification(userWalletId)
    }

    suspend fun decline(userWalletId: UserWalletId) {
        walletsRepository.declineSeedPhraseNotification(userWalletId)
    }
}