package com.tangem.tap.common.feedback

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.domain.feedback.FeedbackManagerFeatureToggles

internal class DefaultFeedbackManagerFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : FeedbackManagerFeatureToggles {

    override val isLocalLogsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "LOCAL_USER_LOGS_ENABLED")
}