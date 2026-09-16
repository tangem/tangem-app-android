package com.tangem.features.introduction

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface IntroductionComponent : ComposableContentComponent {

    /** @param launchMode [InitScreenLaunchMode.WithCardScan] means an NFC tag opened the app: the scan starts untapped. */
    data class Params(
        val launchMode: InitScreenLaunchMode,
        val entryReason: AppRoute.Home.EntryReason = AppRoute.Home.EntryReason.Launch,
    )

    interface Factory : ComponentFactory<Params, IntroductionComponent>
}