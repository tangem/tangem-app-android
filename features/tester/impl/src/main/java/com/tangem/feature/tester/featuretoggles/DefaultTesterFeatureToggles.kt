package com.tangem.feature.tester.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.features.tester.api.TesterFeatureToggles

/**
 * Default implementation of Tester feature toggles
 *
 * @property featureTogglesManager manager for getting information about the availability of feature toggles
 *
[REDACTED_AUTHOR]
 */
internal class DefaultTesterFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : TesterFeatureToggles {

    override val isDerivePublicKeysRefactoringEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "DERIVE_PUBLIC_KEYS_REFACTORING_ENABLED")
}