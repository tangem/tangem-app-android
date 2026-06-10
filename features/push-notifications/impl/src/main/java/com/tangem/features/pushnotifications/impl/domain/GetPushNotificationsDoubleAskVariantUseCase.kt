package com.tangem.features.pushnotifications.impl.domain

import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.features.pushnotifications.PushNotificationsFeatureToggles
import javax.inject.Inject

class GetPushNotificationsDoubleAskVariantUseCase @Inject constructor(
    private val pushNotificationsFeatureToggles: PushNotificationsFeatureToggles,
    private val abTestsManager: ABTestsManager,
) {

    operator fun invoke(): DoubleAskVariant {
        if (!pushNotificationsFeatureToggles.isOnboardingPushDoubleAskAbEnabled) {
            return DoubleAskVariant.Off
        }
        val variant = abTestsManager.getValue(AMPLITUDE_ID, DoubleAskVariant.Off.key)
        return DoubleAskVariant.fromKey(variant)
    }

    private companion object {
        const val AMPLITUDE_ID = "twi_1403_onboarding_push_notification_double_ask"
    }
}