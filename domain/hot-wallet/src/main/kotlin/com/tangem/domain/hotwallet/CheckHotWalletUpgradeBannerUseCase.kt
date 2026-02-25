package com.tangem.domain.hotwallet

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.hotwallet.repository.HotWalletRepository
import com.tangem.domain.models.wallet.UserWalletId
import java.util.concurrent.TimeUnit

class CheckHotWalletUpgradeBannerUseCase(
    private val hotWalletRepository: HotWalletRepository,
) {
    suspend operator fun invoke(
        walletId: UserWalletId,
        hasBalance: Boolean,
        shouldShowUpgradeBanner: Boolean,
        closureTimestamp: Long?,
    ): Either<Throwable, Boolean> = try {
        val currentTime = System.currentTimeMillis()
        val creationTimestamp = hotWalletRepository.getWalletCreationTimestamp(walletId)

        val creationTimestampActual = if (creationTimestamp == null) {
            // If creationTimestamp is null (wallet was created before this feature was released),
            // store the current timestamp and use it below
            hotWalletRepository.setWalletCreationTimestamp(walletId, currentTime)
            currentTime
        } else {
            creationTimestamp
        }

        val hasHadFirstTopUp = hotWalletRepository.hasHadFirstTopUp(walletId)

        val daysSinceCreation = TimeUnit.MILLISECONDS.toDays(currentTime - creationTimestampActual)
        val daysSinceClosure = closureTimestamp?.let { TimeUnit.MILLISECONDS.toDays(currentTime - it) }

        // Wallet balance is positive, but the first top-up hasn't been tracked yet
        if (hasBalance && !hasHadFirstTopUp) {
            hotWalletRepository.setHasHadFirstTopUp(walletId, true)
            hotWalletRepository.setShouldShowUpgradeBanner(walletId, true)
            hotWalletRepository.setUpgradeBannerClosureTimestamp(walletId, null)
            hotWalletRepository.markFirstTopUpDetectedThisSession(walletId)
        }

        val shouldShow = when {
            // Banner should be shown (e.g., because of the first top-up in the previous session)
            shouldShowUpgradeBanner -> !hotWalletRepository.isFirstTopUpDetectedThisSession(walletId)
            // Banner was closed; it happened more than BANNER_RESHOW_DAYS (30) days ago
            closureTimestamp != null && daysSinceClosure != null && daysSinceClosure >= BANNER_RESHOW_DAYS -> true
            // Banner hasn't been closed; wallet was created more than BANNER_RESHOW_DAYS (30) days ago
            closureTimestamp == null && daysSinceCreation >= BANNER_RESHOW_DAYS -> true
            else -> false
        }
        shouldShow.right()
    } catch (e: Exception) {
        e.left()
    }

    companion object {
        const val BANNER_RESHOW_DAYS = 30L
    }
}