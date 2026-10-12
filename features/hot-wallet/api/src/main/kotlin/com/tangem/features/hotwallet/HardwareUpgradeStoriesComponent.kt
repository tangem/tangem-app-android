package com.tangem.features.hotwallet

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

/** The upgrade story; however it ends, the Hardware wallet screen replaces it. */
interface HardwareUpgradeStoriesComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val source: AppRoute.HardwareUpgradeStories.Source,
    )

    interface Factory : ComponentFactory<Params, HardwareUpgradeStoriesComponent>
}