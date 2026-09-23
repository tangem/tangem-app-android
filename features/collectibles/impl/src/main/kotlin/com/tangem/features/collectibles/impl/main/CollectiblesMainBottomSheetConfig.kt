package com.tangem.features.collectibles.impl.main

import com.tangem.domain.models.TokenReceiveConfig
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface CollectiblesMainBottomSheetConfig {

    @Serializable
    data object TopUp : CollectiblesMainBottomSheetConfig

    @Serializable
    data class Receive(val config: TokenReceiveConfig) : CollectiblesMainBottomSheetConfig
}