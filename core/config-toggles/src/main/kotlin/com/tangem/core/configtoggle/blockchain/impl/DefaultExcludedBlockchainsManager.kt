package com.tangem.core.configtoggle.blockchain.impl

import com.tangem.core.configtoggle.blockchain.MutableExcludedBlockchainsManager
import com.tangem.core.configtoggle.storage.TogglesStorage
import com.tangem.core.configtoggle.utils.associateToggles
import com.tangem.core.configtoggle.version.VersionProvider
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store

internal class DefaultExcludedBlockchainsManager(
    private val localTogglesStorage: TogglesStorage,
    private val appPreferencesStore: AppPreferencesStore,
    private val versionProvider: VersionProvider,
) : MutableExcludedBlockchainsManager {

    private var isInitialized: Boolean = false

    private lateinit var currentExcludedBlockchainsIds: Set<String>
    private lateinit var localExcludedBlockchainsIds: Set<String>

    override val excludedBlockchainsIds: Set<String>
        get() {
            if (!isInitialized) error("ExcludedBlockchainsManager is not initialized")

            return currentExcludedBlockchainsIds
        }

    override suspend fun init() {
        localTogglesStorage.populate(path = "configs/excluded_blockchains_config")

        val storedExcludedBlockchainsIds = appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.EXCLUDED_BLOCKCHAINS_KEY,
            default = emptySet(),
        )

        localExcludedBlockchainsIds = localTogglesStorage.toggles
            .associateToggles(currentVersion = versionProvider.get().orEmpty())
            .filterValues { isIncluded -> !isIncluded }
            .keys

        if (storedExcludedBlockchainsIds.isEmpty()) {
            recoverLocalConfig()
        } else {
            currentExcludedBlockchainsIds = storedExcludedBlockchainsIds
        }

        isInitialized = true
    }

    override suspend fun excludeBlockchain(mainnetId: String, isExcluded: Boolean) {
        if (isExcluded) {
            currentExcludedBlockchainsIds += mainnetId
        } else {
            currentExcludedBlockchainsIds -= mainnetId
        }

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
        appPreferencesStore.store(
            PreferencesKeys.EXCLUDED_BLOCKCHAINS_KEY,
            currentExcludedBlockchainsIds,
        )
    }
}