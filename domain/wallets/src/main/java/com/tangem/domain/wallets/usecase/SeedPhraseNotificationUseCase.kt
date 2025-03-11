package com.tangem.domain.wallets.usecase

import com.tangem.domain.wallets.models.SeedPhraseNotificationsStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository
import kotlinx.coroutines.flow.Flow

class SeedPhraseNotificationUseCase(
    private val walletsRepository: WalletsRepository,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<SeedPhraseNotificationsStatus> {
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

    suspend fun acceptSecond(userWalletId: UserWalletId) {
        walletsRepository.acceptSeedPhraseSecondNotification(userWalletId)
    }

    suspend fun rejectSecond(userWalletId: UserWalletId) {
        walletsRepository.rejectSeedPhraseSecondNotification(userWalletId)
    }
}