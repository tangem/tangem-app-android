package com.tangem.features.hotwallet

import com.tangem.common.routing.AppRoute
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Opens the upgrade story or the Hardware wallet screen, as the upgrade experiment decides. May suspend until the first
 * experiment fetch completes, so launch it in a scope cancelled when the screen goes away.
 */
interface HardwareUpgradeLauncher {

    suspend fun launch(userWalletId: UserWalletId, source: AppRoute.HardwareUpgradeStories.Source)

    /** Warms the upgrade story up if the experiment on the device shows it; call it when an entry point appears. */
    fun prefetch()
}