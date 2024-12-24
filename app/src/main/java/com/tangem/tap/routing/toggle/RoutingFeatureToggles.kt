package com.tangem.tap.routing.toggle

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoutingFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) {

    val isNavigationRefactoringEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "NAVIGATION_REFACTORING")
}