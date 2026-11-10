package com.tangem.domain.collectibles.models

sealed interface CollectiblePendingPack {

    val txId: String

    val isDismissible: Boolean

    data class PaymentInProgress(override val txId: String) : CollectiblePendingPack {
        override val isDismissible: Boolean = false
    }

    /** @property machineId the machine's external id, e.g. `pokemon_50` */
    data class ReadyToOpen(
        override val txId: String,
        val machineId: String,
        val isTurboModeEnabled: Boolean,
    ) : CollectiblePendingPack {
        override val isDismissible: Boolean = false
    }

    data class OpenFailed(override val txId: String) : CollectiblePendingPack {
        override val isDismissible: Boolean = true
    }

    data class Refunding(override val txId: String) : CollectiblePendingPack {
        override val isDismissible: Boolean = true
    }
}