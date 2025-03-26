package com.tangem.sdk.api.featuretoggles

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import javax.inject.Inject

internal class DefaultCardSdkFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : CardSdkFeatureToggles {

    override val isNewAttestationEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "NEW_ATTESTATION_ENABLED")
}