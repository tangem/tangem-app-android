package com.tangem.domain.wallets.registration

import com.tangem.domain.models.wallet.UserWallet

/**
 * Domain port for triggering Tangem Auth Service wallet registration.
 *
 * Fire-and-forget from the caller's perspective: implementations must not throw — registration is
 * best-effort and retried later.
 */
interface WalletRegistrationTrigger {

    /** Registers a freshly created/imported MOBILE (hot) wallet while its unlock context is fresh. */
    suspend fun onMobileWalletCreated(userWallet: UserWallet.Hot)
}