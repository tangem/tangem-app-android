package com.tangem.features.collectibles.impl.onboarding

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface CollectiblesOnboardingBottomSheetConfig {

    @Serializable
    data object RegionRestricted : CollectiblesOnboardingBottomSheetConfig
}