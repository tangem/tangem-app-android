package com.tangem.core.toggle.storage

import com.tangem.datasource.asset.loader.AssetLoader
import kotlin.properties.Delegates

/**
 * Storage implementation for storing local feature toggles.
 * Feature toggles are declared in file [LOCAL_CONFIG_PATH].
 *
 * @property assetLoader asset loader
 *
[REDACTED_AUTHOR]
 */
internal class LocalTogglesStorage(
    private val assetLoader: AssetLoader,
) : TogglesStorage {

    override var toggles: List<Toggle> by Delegates.notNull()
        private set

    override suspend fun populate(path: String) {
        toggles = assetLoader.loadList<Toggle>(path)
    }
}