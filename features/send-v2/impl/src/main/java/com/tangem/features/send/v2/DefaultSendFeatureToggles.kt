package com.tangem.features.send.v2

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.send.v2.api.SendFeatureToggles

internal class DefaultSendFeatureToggles(
    @Suppress("UnusedPrivateMember") private val featureToggles: FeatureTogglesManager,
) : SendFeatureToggles