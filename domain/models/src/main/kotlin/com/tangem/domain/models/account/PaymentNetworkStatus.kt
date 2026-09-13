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

 *    contract addresses, so such a network cannot carry currencies at all.
 *  - [Disabled] (backend `DISABLED`): info-only, no contract — currencies only.
 *
 * Every entry represents a single [network]; the contained currencies all belong to it.
 */
@Serializable
sealed class PaymentNetworkStatus {

    /** The blockchain network this status describes. */
    abstract val network: Network

    /**
     * @property depositAddress the network's deposit address; never empty — an `ENABLED` network the backend
     * has not provided an address for is dropped by the data layer instead of becoming [Available].
     * @property chainId the backend's chain id for this network, as required by the withdrawal endpoints. Taken
     * from the response rather than derived from [network]: non-EVM payment networks (Tron) have no chain id in
     * the blockchain SDK.
     */
    @Serializable
    data class Available(
        override val network: Network,
        val depositAddress: String,
        val chainId: Long,
        val cryptoCurrencyStatuses: List<CryptoCurrencyStatus>,
    ) : PaymentNetworkStatus()

    @Serializable
    data class NotIssued(
        override val network: Network,
    ) : PaymentNetworkStatus()

    @Serializable
    data class Disabled(
        override val network: Network,
        val cryptoCurrencies: List<CryptoCurrency>,
    ) : PaymentNetworkStatus()
}