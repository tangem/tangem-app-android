package com.tangem.features.collectibles.impl.packopening

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface CollectiblesConfirmOrderBottomSheetConfig {

    @Serializable
    data class PackUnopened(val txId: String) : CollectiblesConfirmOrderBottomSheetConfig

    @Serializable
    data object LowInventory : CollectiblesConfirmOrderBottomSheetConfig
}