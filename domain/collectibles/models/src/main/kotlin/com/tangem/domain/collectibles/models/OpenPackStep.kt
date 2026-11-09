package com.tangem.domain.collectibles.models

import java.math.BigDecimal

sealed interface OpenPackStep {

    data object NotStarted : OpenPackStep

    data object Generating : OpenPackStep

    data object Confirming : OpenPackStep

    data object Opening : OpenPackStep

    data class Opened(val pack: OpenedCollectiblePack) : OpenPackStep

    data class AutoBuybacked(val amount: BigDecimal, val pack: OpenedCollectiblePack?) : OpenPackStep

    sealed interface Failed : OpenPackStep {

        data object LowInventory : Failed

        /** Failed before the payment was sent: nothing spent. */
        data object PurchaseFailed : Failed

        /** Opening failed for a reason a retry may fix: the pack is paid for, but it is not opened. */
        data class PackUnopened(val txId: String) : Failed

        /** The pack cannot be opened: the purchase expired or the gacha refused it, so a retry cannot help. */
        data object OpenFailed : Failed

        /** The purchase was paid back instead of opened. */
        data object Refunded : Failed

        /** The pack will not be opened and the payment is being returned. */
        data object Refunding : Failed

        /** The purchase ended in a state this flow cannot reach, such as a sold card. */
        data object Unexpected : Failed
    }
}