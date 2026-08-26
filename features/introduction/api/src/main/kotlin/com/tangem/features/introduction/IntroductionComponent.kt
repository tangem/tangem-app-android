package com.tangem.features.introduction

import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface IntroductionComponent : ComposableContentComponent {

    /**
     * @param launchMode how the app was started. [InitScreenLaunchMode.WithCardScan] means an NFC tap brought
     * the user here and the card scan is expected to start on its own, which this screen cannot do yet.
     */
    data class Params(val launchMode: InitScreenLaunchMode)

    interface Factory : ComponentFactory<Params, IntroductionComponent>
}