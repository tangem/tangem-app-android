package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.card.IsWalletBackupProblematicUseCase
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.backup.CardBackupConverter
import com.tangem.domain.wallets.models.errors.WalletCardsBackupError
import com.tangem.domain.wallets.repository.WalletCardsBackupRepository

/**
 * Reports the card of [UserWallet.Cold.scanResponse] seen outside onboarding, together with the other cards of the
 * wallet this device knows, so the backend can track a backup that was never completed.
 *
 * A wallet being added is reported whatever its backup state. A wallet that is already saved is reported only while
 * its backup has a problem: either the seen card shows one, or a card of the wallet seen earlier does. A card that
 * cannot be backed up has nothing to report. The card's role is unknown here, so it is not sent.
 */
class ReportSeenWalletCardUseCase(
    private val walletCardsBackupRepository: WalletCardsBackupRepository,
    private val isWalletBackupProblematicUseCase: IsWalletBackupProblematicUseCase,
) {

    suspend operator fun invoke(
        userWallet: UserWallet.Cold,
        isNewWallet: Boolean,
    ): Either<WalletCardsBackupError, Unit> {
        val card = userWallet.scanResponse.card
        if (!card.settings.isBackupAllowed || card.backupStatus == null) return Unit.right()
        if (!isNewWallet && !hasBackupProblem(userWallet)) return Unit.right()

        return walletCardsBackupRepository.saveSeenWalletCard(
            userWalletId = userWallet.walletId,
            card = CardBackupConverter.convert(card = card, role = null),
            usedSeed = userWallet.isImported,
        )
    }

    private suspend fun hasBackupProblem(userWallet: UserWallet.Cold): Boolean {
        val hasStartedBackup = userWallet.scanResponse.card.backupStatus != CardDTO.BackupStatus.NoBackup

        return hasStartedBackup && isWalletBackupProblematicUseCase(userWallet) ||
            walletCardsBackupRepository.hasCardWithIncompleteBackup(userWallet.walletId)
    }
}