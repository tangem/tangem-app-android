package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import com.tangem.domain.card.IsWalletBackupProblematicUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.backup.CardBackupConverter
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.domain.wallets.models.errors.WalletCardsBackupError
import com.tangem.domain.wallets.repository.WalletCardsBackupRepository

/**
 * Reports a wallet whose backup the app locally knows to be broken, but which the backend has no record of — a
 * wallet activated by a build that predates card-backup reporting. Until it is reported, the breakage is known to
 * this device alone.
 *
 * Only the scanned card is reported: a saved wallet keeps the rest as bare ids
 * ([UserWallet.Cold.cardsInWallet]), and a card's backup status can only be read by tapping it.
 *
 * Goes to the repository rather than [GetWalletBackupIntegrityUseCase], which stops at
 * [com.tangem.domain.wallets.models.backup.WalletBackupIntegrity.LocallyDetectedProblem] without asking the
 * backend — exactly the case this one has to look past.
 */
class ReportMissingWalletCardsBackupUseCase(
    private val walletCardsBackupRepository: WalletCardsBackupRepository,
    private val reportWalletCardsBackupUseCase: ReportWalletCardsBackupUseCase,
    private val isWalletBackupProblematicUseCase: IsWalletBackupProblematicUseCase,
) {

    suspend operator fun invoke(userWallet: UserWallet): Either<WalletCardsBackupError, Unit> {
        if (userWallet !is UserWallet.Cold) return Unit.right()

        // firmware without backup support: the same null check BackupValidator treats as "nothing to validate"
        if (userWallet.scanResponse.card.backupStatus == null) return Unit.right()

        if (!isWalletBackupProblematicUseCase(userWallet)) return Unit.right()

        return walletCardsBackupRepository.getWalletCards(userWallet.walletId).flatMap { knownCards ->
            if (knownCards.isEmpty()) report(userWallet) else Unit.right()
        }
    }

    private suspend fun report(userWallet: UserWallet.Cold): Either<WalletCardsBackupError, Unit> {
        val card = CardBackupConverter.convert(
            card = userWallet.scanResponse.card,
            role = WalletCardBackup.Role.PRIMARY,
        )

        return reportWalletCardsBackupUseCase(
            userWalletId = userWallet.walletId,
            cards = listOf(card),
            // an imported wallet is one created from a seed — the only source left once the activation session is over
            usedSeed = userWallet.isImported,
        )
    }
}