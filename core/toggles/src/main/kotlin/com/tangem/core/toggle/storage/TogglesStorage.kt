package com.tangem.core.toggle.storage

/**
 * Component that initializes and stores a list of feature toggles
 *
 * @author Andrew Khokhlov on 25/01/2023
 */
internal interface TogglesStorage {

    /** List of feature toggles */
    val toggles: List<Toggle>

    /** Populate the storage with toggles */
    suspend fun populate(path: String)
}
