package com.tangem.features.collectibles.impl.main

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface CollectiblesMainBottomSheetConfig {

    @Serializable
    data object TopUp : CollectiblesMainBottomSheetConfig
}