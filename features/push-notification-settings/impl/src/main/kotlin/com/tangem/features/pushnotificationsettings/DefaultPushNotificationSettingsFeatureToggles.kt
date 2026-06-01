package com.tangem.features.pushnotificationsettings

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultPushNotificationSettingsFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : PushNotificationSettingsFeatureToggles {

    override val isPushNotificationSettingsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1403_PUSH_NOTIFICATION_SETTINGS_ENABLED)
}