package com.tangem.domain.wallets.registration

import com.tangem.domain.models.wallet.UserWallet

/**
 * Asks the user to link a wallet to this device in a sheet shown over any screen, for a feature that got (or knows it
 * will get) an auth error because the wallet is not linked.
 */
interface WalletLinkRequester {

    /** Suspends until the sheet closes: [Result.Linked] once the wallet is linked, [Result.Dismissed] otherwise. */
    suspend fun requestLink(userWallet: UserWallet): Result

    enum class Result {
        Linked,
        Dismissed,
    }
}