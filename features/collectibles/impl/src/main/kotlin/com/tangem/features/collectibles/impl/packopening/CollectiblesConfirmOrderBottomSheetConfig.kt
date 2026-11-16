package com.tangem.features.collectibles.impl.packopening

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface CollectiblesConfirmOrderBottomSheetConfig {

    @Serializable
    data class PackUnopened(val txId: String) : CollectiblesConfirmOrderBottomSheetConfig

    @Serializable
    data object LowInventory : CollectiblesConfirmOrderBottomSheetConfig

    /** @property amount the payout and [price] one more spin, both formatted with their token */
    @Serializable
    data class AutoBuyback(val amount: String, val price: String) : CollectiblesConfirmOrderBottomSheetConfig
}