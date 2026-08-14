package com.tangem.domain.models.account

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import kotlinx.serialization.Serializable

/**
 * A blockchain network attached to a payment account (multichain), tagged by its issuance status.
 * The status subtype determines what data the network carries:
 *  - [Available] (backend `ENABLED`): contract open, deposit address exists — full [CryptoCurrencyStatus]s
 *    (currency + balance + receive address).

 *  - [Disabled] (backend `DISABLED`): info-only, no contract — currencies only.
 *
 * Every entry represents a single [network]; the contained currencies all belong to it.
 */
@Serializable
sealed class PaymentNetworkStatus {

    /** The blockchain network this status describes. */
    abstract val network: Network

    /**
     * @property depositAddress the network's deposit address; empty when the backend reports the
     * network `ENABLED` but has not provided an address (receive actions must stay disabled then).
     */
    @Serializable
    data class Available(
        override val network: Network,
        val depositAddress: String,
        val cryptoCurrencyStatuses: List<CryptoCurrencyStatus>,
    ) : PaymentNetworkStatus()

    @Serializable
    data class NotIssued(
        override val network: Network,
        val cryptoCurrencies: List<CryptoCurrency>,
    ) : PaymentNetworkStatus()

    @Serializable
    data class Disabled(
        override val network: Network,
        val cryptoCurrencies: List<CryptoCurrency>,
    ) : PaymentNetworkStatus()
}