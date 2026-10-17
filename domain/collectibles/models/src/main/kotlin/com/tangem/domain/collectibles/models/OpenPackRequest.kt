package com.tangem.domain.collectibles.models

sealed interface OpenPackRequest {

    /** @property machineId the machine's external id, e.g. `pokemon_50` */
    data class Buy(val machineId: String, val isTurboEnabled: Boolean) : OpenPackRequest

    /** Opens a pack whose payment was sent but which was left unopened. */
    data class Reopen(val txId: String) : OpenPackRequest
}