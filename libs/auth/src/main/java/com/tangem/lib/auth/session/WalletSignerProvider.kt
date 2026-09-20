package com.tangem.lib.auth.session

/**
 * Resolves the [WalletSigner] of an already-saved wallet, so a caller that only knows a wallet id can
 * sign a server nonce with it. Implemented in the app layer, where the hot wallet SDK and the wallets
 * list live — the auth library stays free of both.
 */
interface WalletSignerProvider {

    /**
     * @param walletId Base64 `UserWalletId`, the form the auth service addresses wallets by
     * @return `null` when no signable wallet with that id is available on this device
     */
    suspend fun signerFor(walletId: String): WalletSigner?
}