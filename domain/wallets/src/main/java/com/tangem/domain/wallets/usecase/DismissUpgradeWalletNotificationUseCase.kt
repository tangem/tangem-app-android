package com.tangem.domain.wallets.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository

class DismissUpgradeWalletNotificationUseCase(
    private val walletsRepository: WalletsRepository,
) {
    suspend operator fun invoke(userWalletId: UserWalletId) {
        walletsRepository.dismissUpgradeWalletNotification(userWalletId)
    }
}