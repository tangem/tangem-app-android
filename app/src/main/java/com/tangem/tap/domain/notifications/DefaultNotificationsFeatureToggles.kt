package com.tangem.tap.domain.notifications

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.notifications.toggles.NotificationsFeatureToggles

internal class DefaultNotificationsFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : NotificationsFeatureToggles {
    override val isNotificationsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("PUSH_NOTIFICATIONS_ENABLED")
}