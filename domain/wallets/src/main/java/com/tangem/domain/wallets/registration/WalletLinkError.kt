package com.tangem.domain.wallets.registration

sealed interface WalletLinkError {

    /** The user dismissed the NFC scan or the hot wallet authentication — not a failure to report. */
    data object Cancelled : WalletLinkError

    /** Twin and Visa cards are not registered with the Tangem Auth Service. */
    data object NotSupported : WalletLinkError

    data class Failed(val reason: String) : WalletLinkError
}