package com.tangem.sdk.api.featuretoggles

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import javax.inject.Inject

internal class DefaultCardSdkFeatureToggles @Inject constructor(
    @Suppress("UnusedPrivateMember") private val featureTogglesManager: FeatureTogglesManager,
) : CardSdkFeatureToggles