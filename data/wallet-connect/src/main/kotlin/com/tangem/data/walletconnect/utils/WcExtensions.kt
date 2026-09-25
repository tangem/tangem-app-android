package com.tangem.data.walletconnect.utils

import com.reown.walletkit.client.Wallet

/**
 * Returns the dApp URL to show to the user (and to log) for this Verify context.
 *
 * The Verify-attested `origin` wins whenever it is present. When Verify could not attest the dApp (`UNKNOWN`
 * validation, empty `origin`) the fallback is the dApp's own, unverified [metadataUrl] — never `verifyUrl`:
 * that field is the address of the Verify service itself (`verify.walletconnect.org`), and showing it as the
 * dApp's domain would present an unverified dApp under a WalletConnect-owned, trustworthy-looking host.
 * The security verdict is decided separately by the caller; this is display data only.
 */
fun Wallet.Model.VerifyContext.getDappOriginUrl(metadataUrl: String?): String {
    return if (this.validation == Wallet.Model.Validation.INVALID || this.isScam == true) {
        this.origin
    } else {
        this.origin.ifEmpty { metadataUrl.orEmpty() }
    }
}
