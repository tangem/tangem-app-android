package com.tangem.domain.hotwallet

import com.tangem.domain.hotwallet.repository.HotWalletRepository
import com.tangem.domain.models.wallet.UserWalletId

class SetHotWalletBalanceThresholdReachedUseCase(
    private val hotWalletRepository: HotWalletRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, isReached: Boolean) {
        hotWalletRepository.setBalanceThresholdReached(userWalletId, isReached)
    }
}