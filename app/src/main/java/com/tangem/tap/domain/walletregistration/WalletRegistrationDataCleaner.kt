package com.tangem.tap.domain.walletregistration

import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

/** Unregisters deleted wallets from the Tangem Auth Service. Best-effort — see [WalletRegistrationLauncher.unregister]. */
internal class WalletRegistrationDataCleaner @Inject constructor(
    private val walletRegistrationLauncher: WalletRegistrationLauncher,
) : UserWalletDataCleaner {

    override suspend fun clear(userWalletIds: List<UserWalletId>) {
        userWalletIds.forEach { walletRegistrationLauncher.unregister(it) }
    }
}