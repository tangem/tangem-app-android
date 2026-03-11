package com.tangem.domain.hotwallet

import com.tangem.domain.hotwallet.repository.HotWalletRepository
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

class GetUpgradeBannerClosureTimestampUseCase(
    private val hotWalletRepository: HotWalletRepository,
) {
    operator fun invoke(userWalletId: UserWalletId): Flow<Long?> {
        return hotWalletRepository.upgradeBannerClosureTimestamp(userWalletId)
    }
}