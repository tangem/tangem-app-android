package com.tangem.domain.staking.model.ethpool

/**
 * Configuration for P2PEthPool Ethereum staking network.
 *
 * Change [USE_TESTNET] to switch between testnet and mainnet.
 */
object P2PEthPoolStakingConfig {

    const val USE_TESTNET: Boolean = true

    val activeNetwork: P2PEthPoolNetwork
        get() = if (USE_TESTNET) P2PEthPoolNetwork.TESTNET else P2PEthPoolNetwork.MAINNET
}