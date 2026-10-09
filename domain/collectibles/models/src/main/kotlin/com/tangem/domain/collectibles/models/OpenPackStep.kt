package com.tangem.domain.collectibles.models

sealed interface OpenPackStep {

    data object Generating : OpenPackStep

    data object Confirming : OpenPackStep

    data object Opening : OpenPackStep

    data class Opened(val pack: OpenedCollectiblePack) : OpenPackStep

    sealed interface Failed : OpenPackStep {

        data object LowInventory : Failed

        /** Failed before the payment was sent: nothing spent. */
        data object PurchaseFailed : Failed

        /** Failed after the payment was sent: the pack may be paid for, but it is not opened. */
        data class PackUnopened(val txId: String) : Failed
    }
}