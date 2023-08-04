package com.tangem.domain.tokens.model

import com.tangem.domain.tokens.models.Network

/**
 * Represents a group of cryptocurrencies associated with a specific network.
 *
 * This class encapsulates a collection of cryptocurrency statuses, all of which are part of the same blockchain network.
 *
 * @property network The blockchain network associated with the group.
 * @property currencies A set of cryptocurrency statuses that belong to the network.
 */
data class NetworkGroup(
    val network: Network,
    val currencies: Set<CryptoCurrencyStatus>,
)