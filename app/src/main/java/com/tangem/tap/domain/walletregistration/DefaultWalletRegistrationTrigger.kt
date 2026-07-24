package com.tangem.tap.domain.walletregistration

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.registration.WalletRegistrationTrigger
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class DefaultWalletRegistrationTrigger @Inject constructor(
    private val launcher: WalletRegistrationLauncher,
    private val appCoroutineScope: AppCoroutineScope,
) : WalletRegistrationTrigger {

    override fun onMobileWalletCreated(userWallet: UserWallet.Hot) {
        // Dispatch on the application scope, not the caller's: the wallet is registered right after it
        // is saved from a screen model, and that model's scope is cancelled the moment onboarding
        // navigates to the wallet screen. registerMobile is already log-only / idempotent.
        appCoroutineScope.launch {
            launcher.registerMobile(userWallet)
        }
    }
}