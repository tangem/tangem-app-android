package com.tangem.features.pushnotifications.impl.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.features.pushnotifications.api.featuretoggles.PushNotificationsFeatureToggles

internal class DefaultPushNotificationsFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : PushNotificationsFeatureToggles {
    override val isPushNotificationsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("PUSH_NOTIFICATIONS_ENABLED")
}