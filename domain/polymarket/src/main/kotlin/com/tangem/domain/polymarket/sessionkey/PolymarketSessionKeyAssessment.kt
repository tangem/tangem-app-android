package com.tangem.domain.polymarket.sessionkey

import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import java.time.Instant

/**
 * @property sessionKeys the wallet's `sessionKeys` from the BFF
 * @property localKeyAddress the key this device holds for the wallet, `null` if there is none
 * @property signers the signers Polymarket currently honours, `null` if they could not be read
 */
data class PolymarketSessionKeySnapshot(
    val sessionKeys: List<PolymarketSessionKey>,
    val localKeyAddress: String?,
    val deviceId: String,
    val signers: List<PolymarketSessionSigner>?,
    val now: Instant,
)

data class PolymarketSessionKeyAssessment(
    val deviceState: PolymarketSessionKeyDeviceState,
    val reconciliation: PolymarketSessionKeyReconciliation,
)

sealed interface PolymarketSessionKeyDeviceState {

    data object OfferEnable : PolymarketSessionKeyDeviceState

    data class Enabling(val sessionKey: PolymarketSessionKey) : PolymarketSessionKeyDeviceState

    data class Revoking(val sessionKey: PolymarketSessionKey) : PolymarketSessionKeyDeviceState

    data class Active(
        val sessionKey: PolymarketSessionKey,
        val isRenewDue: Boolean,
    ) : PolymarketSessionKeyDeviceState

    data class Disabled(
        val sessionKey: PolymarketSessionKey,
        val isRenewDue: Boolean,
    ) : PolymarketSessionKeyDeviceState

    data class RenewalRequired(val sessionKey: PolymarketSessionKey) : PolymarketSessionKeyDeviceState

    data class Reenable(val sessionKey: PolymarketSessionKey) : PolymarketSessionKeyDeviceState

    data class RepairByRenewal(val sessionKey: PolymarketSessionKey) : PolymarketSessionKeyDeviceState

    data class WaitingForPrevious(val previous: PolymarketSessionKey) : PolymarketSessionKeyDeviceState

    sealed interface Replace : PolymarketSessionKeyDeviceState {

        val previous: PolymarketSessionKey

        data class ByRevocation(override val previous: PolymarketSessionKey) : Replace

        data class ByOverwrite(override val previous: PolymarketSessionKey) : Replace
    }
}

sealed interface PolymarketSessionKeyReconciliation {

    data object Unavailable : PolymarketSessionKeyReconciliation

    /**
     * @property foreignSigners signers registered around the BFF; each one trades the whole deposit wallet
     * @property missingOnExchange addresses `ACTIVE` at the BFF that Polymarket no longer honours
     */
    data class Checked(
        val foreignSigners: List<PolymarketSessionSigner>,
        val missingOnExchange: Set<String>,
    ) : PolymarketSessionKeyReconciliation
}