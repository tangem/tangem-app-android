package com.tangem.core.configtoggle.storage

/**
 * Component that initializes and stores a list of feature toggles
 *
[REDACTED_AUTHOR]
 */
internal interface TogglesStorage {

    /** List of feature toggles */
    val toggles: List<ConfigToggle>

    /** Populate the storage with toggles */
    suspend fun populate(path: String)
}