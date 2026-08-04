package com.tangem.domain.wallets.usecase

import com.tangem.domain.card.IsWalletBackupProblematicUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.models.backup.CardBackupStatus
import com.tangem.domain.wallets.models.backup.WalletBackupIntegrity
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.domain.wallets.repository.WalletCardsBackupRepository

/**
 * Decides whether a wallet's backup can be trusted, and what to ask the user to do about it.
 *
 * Local state wins over the backend: the app can only ever know *more* than the backend about a wallet it holds,
 * so the backend is consulted purely to recover knowledge the app has lost — a reinstall, cleared data, or a
 * login from another device.
 *
 * Wallets that cannot have a card backup return [WalletBackupIntegrity.NotApplicable], so callers may pass every
 * wallet they have without pre-filtering.
 */
class GetWalletBackupIntegrityUseCase(
    private val walletCardsBackupRepository: WalletCardsBackupRepository,
    private val isWalletBackupProblematicUseCase: IsWalletBackupProblematicUseCase,
) {

    suspend operator fun invoke(userWallet: UserWallet): WalletBackupIntegrity {
        if (userWallet !is UserWallet.Cold) return WalletBackupIntegrity.NotApplicable

        // the same null check BackupValidator treats as "nothing to validate": firmware without backup support
        if (userWallet.scanResponse.card.backupStatus == null) return WalletBackupIntegrity.NotApplicable

        if (isWalletBackupProblematicUseCase(userWallet)) return WalletBackupIntegrity.LocallyDetectedProblem

        return walletCardsBackupRepository.getWalletCards(userWallet.walletId).fold(
            ifLeft = { WalletBackupIntegrity.Undetermined },
            ifRight = ::resolve,
        )
    }

    private fun resolve(cards: List<WalletCardBackup>): WalletBackupIntegrity {
        if (cards.isEmpty()) return WalletBackupIntegrity.RecommendedRescan

        val statuses = cards.map { it.backupStatus }

        return when {
            statuses.contains(CardBackupStatus.CARD_LINKED) -> WalletBackupIntegrity.MandatoryRescan
            statuses.all { it == CardBackupStatus.ACTIVE } -> WalletBackupIntegrity.FullyActivated
            statuses.contains(CardBackupStatus.ACTIVE) -> WalletBackupIntegrity.MandatoryRescan
            else -> WalletBackupIntegrity.RecommendedRescan
        }
    }
}