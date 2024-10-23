package com.tangem.core.toggle.blockchain.impl

import com.tangem.core.toggle.blockchain.MutableExcludedBlockchainsManager
import com.tangem.core.toggle.storage.TogglesStorage
import com.tangem.core.toggle.utils.associateToggles
import com.tangem.core.toggle.version.VersionProvider
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.datasource.local.preferences.utils.storeObjectMap

internal class DefaultExcludedBlockchainsManager(
    private val localTogglesStorage: TogglesStorage,
    private val appPreferencesStore: AppPreferencesStore,
    private val versionProvider: VersionProvider,
) : MutableExcludedBlockchainsManager {

    private lateinit var currentExcludedBlockchainsIds: MutableMap<String, Boolean>
    private lateinit var localExcludedBlockchainsIds: MutableMap<String, Boolean>

    override val excludedBlockchains: Map<String, Boolean>
        get() = currentExcludedBlockchainsIds

    override suspend fun init() {
        localTogglesStorage.populate(path = "configs/excluded_blockchains_config")

        val storedExcludedBlockchainsIds = appPreferencesStore.getObjectMapSync<Boolean>(
            key = PreferencesKeys.EXCLUDED_BLOCKCHAINS_KEY,
        )

        localExcludedBlockchainsIds = localTogglesStorage.toggles
            .associateToggles(currentVersion = versionProvider.get().orEmpty())
            .toMutableMap()

        currentExcludedBlockchainsIds = localExcludedBlockchainsIds
            .mapValues { (id, isExcluded) ->
                storedExcludedBlockchainsIds[id] ?: isExcluded
            }
            .toMutableMap()
    }

    override suspend fun toggleBlockchain(mainnetId: String, isExcluded: Boolean) {
        currentExcludedBlockchainsIds[mainnetId] = isExcluded

        storeCurrent()
    }

    override fun isMatchLocalConfig(): Boolean {
        return currentExcludedBlockchainsIds == localExcludedBlockchainsIds
    }

    override suspend fun recoverLocalConfig() {
        currentExcludedBlockchainsIds = localExcludedBlockchainsIds

        storeCurrent()
    }

    private suspend fun storeCurrent() {
        appPreferencesStore.storeObjectMap(
            PreferencesKeys.EXCLUDED_BLOCKCHAINS_KEY,
            currentExcludedBlockchainsIds,
        )
    }
}
