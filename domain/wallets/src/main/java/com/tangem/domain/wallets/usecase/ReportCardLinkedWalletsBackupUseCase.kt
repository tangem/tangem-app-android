package com.tangem.domain.wallets.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.backup.CardBackupConverter
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.domain.wallets.repository.WalletCardsBackupRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Reports, once per wallet, a saved wallet whose card is stuck in `CardLinked` — typically left so by a build that
 * predates card-backup reporting — so another device or another card of the wallet can see the problem.
 *
 * Only the stored card is reported: the rest of the wallet's cards are kept as bare ids. It is the primary card,
 * since only the primary card is ever left `CardLinked`. A locked wallet is skipped until it is unlocked, because
 * its card is stored without wallets and the curves would be lost.
 */
class ReportCardLinkedWalletsBackupUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val walletCardsBackupRepository: WalletCardsBackupRepository,
    private val reportWalletCardsBackupUseCase: ReportWalletCardsBackupUseCase,
) {

    private val mutex = Mutex()

    // an undelivered report stays queued and is resent by the repository, so it is not re-queued in this process
    private val attemptedWalletIds = mutableSetOf<UserWalletId>()

    /** Watches the saved wallets for as long as the caller's scope lives */
    suspend operator fun invoke() {
        userWalletsListRepository.userWallets
            .filterNotNull()
            .collect { userWallets -> reportUnreported(userWallets) }
    }

    private suspend fun reportUnreported(userWallets: List<UserWallet>) = mutex.withLock {
        userWallets
            .filterIsInstance<UserWallet.Cold>()
            .filter { !it.isLocked && it.scanResponse.card.backupStatus is CardDTO.BackupStatus.CardLinked }
            .filter { attemptedWalletIds.add(it.walletId) }
            .forEach { report(it) }
    }

    private suspend fun report(userWallet: UserWallet.Cold) {
        if (walletCardsBackupRepository.isCardLinkedReported(userWallet.walletId)) return

        val card = CardBackupConverter.convert(
            card = userWallet.scanResponse.card,
            role = WalletCardBackup.Role.PRIMARY,
        )

        reportWalletCardsBackupUseCase(
            userWalletId = userWallet.walletId,
            cards = listOf(card),
            usedSeed = userWallet.isImported,
        ).onRight {
            walletCardsBackupRepository.setCardLinkedReported(userWallet.walletId)
        }
    }
}