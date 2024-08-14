package com.tangem.tap.features.home.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import javax.inject.Inject

internal class HomeFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) {

    val isCallbacksRefactoringEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "HOME_SCREEN_CALLBACKS_REFACTORING_ENABLED")
}