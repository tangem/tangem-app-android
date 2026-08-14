package com.tangem.domain.wallets.models.backup

/**
 * Verdict on whether a wallet's backup is trustworthy, and what the user should be asked to do about it.
 *
 * Produced by combining the local backup state with the cards the backend knows about, local state winning.
 */
sealed interface WalletBackupIntegrity {

    /**
     * Local state already proves the backup is broken. The backend answer is not consulted at all — the app
     * knows more about this wallet than the backend does.
     */
    data object LocallyDetectedProblem : WalletBackupIntegrity

    /**
     * The backend reports cards in inconsistent states. A potential threat: the user must rescan, the warning
     * cannot be dismissed.
     */
    data object MandatoryRescan : WalletBackupIntegrity

    /**
     * The backend has no or incomplete information about the wallet. A rescan is recommended but the warning
     * can be dismissed.
     */
    data object RecommendedRescan : WalletBackupIntegrity

    /** The backend reports every card as active. Nothing to do. */
    data object FullyActivated : WalletBackupIntegrity

    /** The wallet cannot have a card backup at all — a hot wallet, or a card whose firmware has no backup. */
    data object NotApplicable : WalletBackupIntegrity

    /**
     * The backend could not be reached, so nothing is known. Deliberately distinct from [RecommendedRescan]:
     * being offline must not surface a warning.
     */
    data object Undetermined : WalletBackupIntegrity
}