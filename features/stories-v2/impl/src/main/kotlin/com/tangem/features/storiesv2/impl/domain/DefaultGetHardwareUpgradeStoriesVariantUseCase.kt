package com.tangem.features.storiesv2.impl.domain

import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.features.storiesv2.GetHardwareUpgradeStoriesVariantUseCase
import com.tangem.features.storiesv2.HardwareUpgradeStoriesVariant
import com.tangem.features.storiesv2.StoriesV2FeatureToggles
import javax.inject.Inject

internal class DefaultGetHardwareUpgradeStoriesVariantUseCase @Inject constructor(
    private val featureToggles: StoriesV2FeatureToggles,
    private val abTestsManager: ABTestsManager,
) : GetHardwareUpgradeStoriesVariantUseCase {

    override suspend fun invoke(): HardwareUpgradeStoriesVariant {
        if (!featureToggles.isHardwareUpgradeStoriesAbEnabled) return HardwareUpgradeStoriesVariant.CONTROL

        val value = abTestsManager.getValue(key = AMPLITUDE_ID, defaultValue = CONTROL_VALUE)
        // Only the exact treatment value shows the story; control, an unknown value or none at all stay safe.
        return if (value == TREATMENT_VALUE) {
            HardwareUpgradeStoriesVariant.TREATMENT
        } else {
            HardwareUpgradeStoriesVariant.CONTROL
        }
    }

    private companion object {
        const val AMPLITUDE_ID = "twi_1357_main_hardware_upgrade_stories"
        const val CONTROL_VALUE = "control"
        const val TREATMENT_VALUE = "treatment"
    }
}