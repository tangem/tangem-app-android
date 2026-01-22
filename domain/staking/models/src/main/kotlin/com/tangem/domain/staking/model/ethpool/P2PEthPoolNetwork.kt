package com.tangem.domain.staking.model.ethpool

/**
 * P2P.org supported networks for pooled staking
 *
 * @property value Network identifier used in API requests
 * @property displayName Human-readable network name
 * @property chainId Ethereum chain ID
 */
enum class P2PEthPoolNetwork(
    val value: String,
    val displayName: String,
    val chainId: Int,
    val stakingNetworkId: String,
    val isTestnet: Boolean,
) {
    /**
     * Ethereum mainnet
     * Chain ID: 1
     */
    MAINNET(
        value = "mainnet",
        displayName = "Ethereum",
        chainId = 1,
        stakingNetworkId = "ethereum",
        isTestnet = false,
    ),

    /**
     * Ethereum testnet (Hoodi)
     * Chain ID: 17000
     */
    TESTNET(
        value = "hoodi",
        displayName = "Hoodi Testnet",
        chainId = 17000,
        stakingNetworkId = "ethereum/test",
        isTestnet = true,
    ),
    ;

    companion object {
        /**
         * Get P2PNetwork by chain ID
         *
         * @param chainId Ethereum chain ID
         * @return P2PNetwork or null if not supported
         */
        fun fromChainId(chainId: Int): P2PEthPoolNetwork? {
            return entries.find { it.chainId == chainId }
        }

        /**
         * Get P2PNetwork by API value
         *
         * @param value API network identifier
         * @return P2PNetwork or null if not found
         */
        fun fromValue(value: String): P2PEthPoolNetwork? {
            return entries.find { it.value == value }
        }

        /**
         * Check if chain ID is supported for P2PEthPool staking
         *
         * @param chainId Ethereum chain ID
         * @return true if supported, false otherwise
         */
        fun isSupported(chainId: Int): Boolean {
            return fromChainId(chainId) != null
        }
    }
}