package com.tangem.domain.wallets.backup

import com.tangem.domain.models.wallet.UserWallet

/**
 * Reports the backup state of a card scanned outside onboarding.
 *
 * Fire-and-forget: the report is sent on an application-level scope, so a scan made from a screen that is closed
 * right after it is still reported.
 */
interface SeenWalletCardReporter {

    /**
     * @param userWallet  the wallet with the scanned card as its card
     * @param isNewWallet `true` when the scan adds the wallet, `false` when the wallet is already saved
     */
    fun report(userWallet: UserWallet.Cold, isNewWallet: Boolean)
}