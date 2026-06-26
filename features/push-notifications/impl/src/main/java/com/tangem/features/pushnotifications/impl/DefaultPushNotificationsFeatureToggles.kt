package com.tangem.features.pushnotifications.impl

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.pushnotifications.PushNotificationsFeatureToggles
import javax.inject.Inject

internal class DefaultPushNotificationsFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : PushNotificationsFeatureToggles {

    override val isOnboardingPushDoubleAskAbEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            FeatureToggles.AND_15596_ONBOARDING_PUSH_NOTIFICATION_DOUBLE_ASK_AB_ENABLED,
        )

    override val isDataPushAsNotificationEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            FeatureToggles.AND_15933_DATA_PUSH_AS_NOTIFICATION_ENABLED,
        )
}