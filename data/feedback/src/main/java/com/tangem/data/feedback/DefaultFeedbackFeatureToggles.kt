package com.tangem.data.feedback

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.feedback.repository.FeedbackFeatureToggles

internal class DefaultFeedbackFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : FeedbackFeatureToggles {

    override val isFeatureEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "USEDESK_FEEDBACK_ENABLED")
}