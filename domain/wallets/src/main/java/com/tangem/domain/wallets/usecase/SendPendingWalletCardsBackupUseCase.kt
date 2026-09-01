package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.wallets.models.errors.WalletCardsBackupError
import com.tangem.domain.wallets.repository.WalletCardsBackupRepository

/**
 * Resends the cards-backup reports that never reached the backend — the device was offline, the request was
 * cut short, the service did not answer.
 *
 * Until a report is delivered, the link between a card and its wallet is known to this device alone, which
 * is precisely the state the reporting exists to make visible. Called on app launch, the next point at which
 * connectivity is worth retrying.
 */
class SendPendingWalletCardsBackupUseCase(
    private val walletCardsBackupRepository: WalletCardsBackupRepository,
) {

    suspend operator fun invoke(): Either<WalletCardsBackupError, Unit> {
        return walletCardsBackupRepository.sendPendingWalletCards()
    }
}