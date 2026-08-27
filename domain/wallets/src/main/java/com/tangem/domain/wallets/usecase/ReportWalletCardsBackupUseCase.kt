package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.domain.wallets.models.errors.WalletCardsBackupError
import com.tangem.domain.wallets.repository.WalletCardsBackupRepository

/**
 * Reports the cards of a wallet and the state of their backup to the backend.
 *
 * Called iteratively over the wallet's activation lifecycle — after the primary card creates the wallet, after
 * each backup card is added, after each card is finalized, and again whenever the app finds it has locally
 * stored an unfinished backup — so the backend always holds the latest known composition.
 *
 * Build [cards] with [com.tangem.domain.wallets.backup.CardBackupConverter].
 */
class ReportWalletCardsBackupUseCase(
    private val walletCardsBackupRepository: WalletCardsBackupRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cards: List<WalletCardBackup>,
        usedSeed: Boolean,
    ): Either<WalletCardsBackupError, Unit> {
        return walletCardsBackupRepository.saveWalletCards(
            userWalletId = userWalletId,
            cards = cards,
            usedSeed = usedSeed,
        )
    }
}