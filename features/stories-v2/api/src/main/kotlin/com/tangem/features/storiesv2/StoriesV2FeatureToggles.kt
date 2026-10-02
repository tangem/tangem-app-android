package com.tangem.features.storiesv2

interface StoriesV2FeatureToggles {

    /** Release gate of the Hardware Upgrade Stories experiment (`twi_1357_main_hardware_upgrade_stories`). */
    val isHardwareUpgradeStoriesAbEnabled: Boolean
}