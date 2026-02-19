package com.tangem.domain.hotwallet

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.hotwallet.repository.HotWalletRepository
import com.tangem.domain.models.wallet.UserWalletId

class CloseHotWalletUpgradeBannerUseCase(
    private val hotWalletRepository: HotWalletRepository,
) {
    suspend operator fun invoke(walletId: UserWalletId): Either<Throwable, Unit> = try {
        val currentTime = System.currentTimeMillis()
        hotWalletRepository.setShouldShowUpgradeBanner(walletId, false)
        hotWalletRepository.setUpgradeBannerClosureTimestamp(walletId, currentTime)
        Unit.right()
    } catch (e: Exception) {
        e.left()
    }
}