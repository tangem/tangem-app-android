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

        return abTestsManager.getValue(key = AMPLITUDE_ID, defaultValue = CONTROL_VALUE).toVariant()
    }

    override fun peek(): HardwareUpgradeStoriesVariant {
        if (!featureToggles.isHardwareUpgradeStoriesAbEnabled) return HardwareUpgradeStoriesVariant.CONTROL

        return abTestsManager.peekValue(key = AMPLITUDE_ID).toVariant()
    }

    // Only the exact treatment value shows the story; control, an unknown value or none at all stay safe.
    private fun String?.toVariant(): HardwareUpgradeStoriesVariant = if (this == TREATMENT_VALUE) {
        HardwareUpgradeStoriesVariant.TREATMENT
    } else {
        HardwareUpgradeStoriesVariant.CONTROL
    }

    private companion object {
        const val AMPLITUDE_ID = "twi_1357_main_hardware_upgrade_stories"
        const val CONTROL_VALUE = "control"
        const val TREATMENT_VALUE = "treatment"
    }
}