package com.tangem.domain.staking.model.ethpool

/**
 * Configuration for P2PEthPool Ethereum staking network.
 *
 * Change [USE_TESTNET] to switch between testnet and mainnet.
 */
object P2PEthPoolStakingConfig {

    const val USE_TESTNET: Boolean = false

    val activeNetwork: P2PEthPoolNetwork
        get() = if (USE_TESTNET) P2PEthPoolNetwork.TESTNET else P2PEthPoolNetwork.MAINNET

    /** Vault addresses returned by the backend that should not be shown to users (test/stub vaults). Stored in lowercase. */
    val TEST_VAULT_ADDRESSES: Set<String> = setOf(
        "0xb72668d6ff7a0e318f83097a754c6aed0f8af034",
    )
}