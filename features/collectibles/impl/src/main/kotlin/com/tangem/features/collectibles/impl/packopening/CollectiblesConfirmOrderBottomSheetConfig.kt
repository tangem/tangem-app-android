package com.tangem.features.collectibles.impl.packopening

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface CollectiblesConfirmOrderBottomSheetConfig {

    @Serializable
    data class PackUnopened(val txId: String) : CollectiblesConfirmOrderBottomSheetConfig

    @Serializable
    data object LowInventory : CollectiblesConfirmOrderBottomSheetConfig

    @Serializable
    data object RefundOnTheWay : CollectiblesConfirmOrderBottomSheetConfig

    /**
     * @property amount the payout and [price] one more spin, both formatted with their token
     * @property cardTitle the sold card's `year set #serial`, null when the card is unknown
     */
    @Serializable
    data class AutoBuyback(
        val amount: String,
        val price: String,
        val cardImageUrl: String?,
        val cardTitle: String?,
    ) : CollectiblesConfirmOrderBottomSheetConfig
}