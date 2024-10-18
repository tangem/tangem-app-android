package com.tangem.core.toggle.storage

/**
 * Component that initializes and stores a list of feature toggles
 *
* [REDACTED_AUTHOR]
 */
internal interface TogglesStorage {

    /** List of feature toggles */
    val toggles: List<Toggle>

    /** Populate the storage with toggles */
    suspend fun populate(path: String)
}
