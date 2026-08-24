package com.tangem.domain.jointaccount.repository

import com.tangem.domain.models.network.Network

/**
 * Provides the fixed, ordered list of blockchain networks supported by joint accounts (Safe multisig).
 *
 * Which networks are supported is a product policy, not a blockchain capability: the list is app-owned
 * and hardcoded in the data layer. The blockchain SDK is used only to map each entry to its network id.
 * Order is part of the contract — callers may render the networks in the returned order.
 */
interface JointAccountSupportedNetworksRepository {

    fun getSupportedNetworks(): List<Network.RawID>
}