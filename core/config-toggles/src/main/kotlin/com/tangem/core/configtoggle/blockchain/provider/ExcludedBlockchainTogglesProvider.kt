package com.tangem.core.configtoggle.blockchain.provider

/**
 * Provider for excluded blockchain toggle entries.
 * This interface allows for easier testing by enabling mock implementations.
 */
internal interface ExcludedBlockchainTogglesProvider {

    /**
     * Returns a map of blockchain IDs to their version strings.
     * The version string is "undefined" for disabled toggles, or a version number like "5.21.0" for enabled ones.
     */
    fun getToggles(): Map<String, String>
}