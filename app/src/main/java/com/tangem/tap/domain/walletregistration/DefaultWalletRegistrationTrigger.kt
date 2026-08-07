package com.tangem.tap.domain.walletregistration

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.registration.WalletRegistrationTrigger
import javax.inject.Inject

internal class DefaultWalletRegistrationTrigger @Inject constructor(
    private val launcher: WalletRegistrationLauncher,
) : WalletRegistrationTrigger {

    override suspend fun onMobileWalletCreated(userWallet: UserWallet.Hot) {
        launcher.registerMobile(userWallet)
    }
}