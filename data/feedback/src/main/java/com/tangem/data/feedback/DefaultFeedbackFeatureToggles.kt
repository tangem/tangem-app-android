package com.tangem.data.feedback

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.feedback.repository.FeedbackFeatureToggles

internal class DefaultFeedbackFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : FeedbackFeatureToggles {

    override val isUsedeskEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(toggle = FeatureToggles.USEDESK_ENABLED)
}