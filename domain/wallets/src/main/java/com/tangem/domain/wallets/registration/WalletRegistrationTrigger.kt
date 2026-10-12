package com.tangem.domain.wallets.registration

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.hot.sdk.model.HotAuth

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

    /**
     * Retries MOBILE registration for hot wallets that have just been unlocked. A wallet whose
     * registration failed earlier (e.g. no session yet at creation) is only re-attempted for
     * NoPassword wallets at startup; Password/Biometry wallets can't sign silently there, so unlocking
     * is the moment a real signing context appears and they can finally register.
     *
     * Non-blocking and idempotent per wallet (already-registered wallets are skipped), so it is safe to
     * call for every wallet unlocked in a batch.
     */
    fun onMobileWalletsUnlocked(userWallets: List<UserWallet.Hot>)

    /**
     * Same as [onMobileWalletsUnlocked] for a wallet unlocked with its [accessCode]: the code the user
     * has just entered signs the registration, so it is not asked for a second time.
     */
    fun onMobileWalletUnlockedWithAccessCode(userWallet: UserWallet.Hot, accessCode: HotAuth.Password)
}