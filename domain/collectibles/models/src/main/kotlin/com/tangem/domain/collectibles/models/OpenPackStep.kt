package com.tangem.domain.collectibles.models

sealed interface OpenPackStep {

    data object NotStarted : OpenPackStep

    data object Generating : OpenPackStep

    data object Confirming : OpenPackStep

    data object Opening : OpenPackStep

    data class Opened(val pack: OpenedCollectiblePack) : OpenPackStep

    sealed interface Failed : OpenPackStep {

        data object LowInventory : Failed

        /** Failed before the payment was sent: nothing spent. */
        data object PurchaseFailed : Failed

        /** Failed once the payment was attempted: the pack may be paid for, but it is not opened. */
        data class PackUnopened(val txId: String) : Failed

        /** The payment never arrived in time, so the purchase is closed. */
        data object Expired : Failed

        /** The purchase was paid back instead of opened. */
        data object Refunded : Failed

        /** The purchase ended in a state this flow cannot reach, such as a sold card. */
        data object Unexpected : Failed
    }
}