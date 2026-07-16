package com.tangem.domain.card

import com.tangem.domain.models.wallet.UserWallet

/**
 * Single source of truth for "this wallet has a backup problem".
 *
 * A wallet is considered problematic when it is backed by a physical card whose backup was not
 * completed correctly (e.g. a card stuck in the `CardLinked` state) or when a backup error has
 * been persisted for it. Such wallets must show a prominent warning and have all top-up
 * operations blocked until the user resolves the issue with support.
 *
 * Local detection only. Can later be backed by a backend flag without changing call sites.
 */
class IsWalletBackupProblematicUseCase(
    private val backupValidator: BackupValidator,
) {

    operator fun invoke(userWallet: UserWallet): Boolean {
        if (userWallet !is UserWallet.Cold) return false
        return userWallet.hasBackupError || !backupValidator.isValidBackupStatus(userWallet.scanResponse.card)
    }
}