package com.tangem.domain.wallets.registration

import com.tangem.domain.models.wallet.UserWallet

/**
 * Domain port for triggering Tangem Auth Service wallet registration.
 *
 * Fire-and-forget from the caller's perspective: implementations must not throw — registration is
 * best-effort and retried later.
 */
interface WalletRegistrationTrigger {

    /**

     *
     * Non-blocking: the implementation dispatches the registration on an application-level scope and
     * returns immediately, so callers on a short-lived scope (a screen model destroyed on navigation)
     * don't cancel the in-flight registration.
     */
    fun onMobileWalletCreated(userWallet: UserWallet.Hot)
}