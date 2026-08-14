package com.tangem.features.pushnotifications.impl.domain

import com.tangem.core.abtests.manager.ABTestsManager
import javax.inject.Inject

class GetPushNotificationsDoubleAskVariantUseCase @Inject constructor(
    private val abTestsManager: ABTestsManager,
) {

    suspend operator fun invoke(): DoubleAskVariant {
        val variant = abTestsManager.getValue(AMPLITUDE_ID, DoubleAskVariant.Off.key)
        return DoubleAskVariant.fromKey(variant)
    }

    private companion object {
        const val AMPLITUDE_ID = "twi_1403_onboarding_push_notification_double_ask"
    }
}