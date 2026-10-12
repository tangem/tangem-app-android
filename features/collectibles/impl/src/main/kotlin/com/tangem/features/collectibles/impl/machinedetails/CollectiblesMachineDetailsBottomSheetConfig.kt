package com.tangem.features.collectibles.impl.machinedetails

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface CollectiblesMachineDetailsBottomSheetConfig {

    @Serializable
    data object TurboInfo : CollectiblesMachineDetailsBottomSheetConfig
}