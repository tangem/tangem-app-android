package com.tangem.core.configtoggle.blockchain.impl

import com.tangem.core.configtoggle.blockchain.MutableExcludedBlockchainsManager
import com.tangem.core.configtoggle.storage.TogglesStorage
import com.tangem.core.configtoggle.utils.associateToggles
import com.tangem.core.configtoggle.version.VersionProvider
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.datasource.local.preferences.utils.storeObjectMap

internal class DefaultExcludedBlockchainsManager(
    private val localTogglesStorage: TogglesStorage,
    private val appPreferencesStore: AppPreferencesStore,
    private val versionProvider: VersionProvider,
) : MutableExcludedBlockchainsManager {

    private var isInitialized: Boolean = false

    private lateinit var currentExcludedBlockchains: MutableMap<String, Boolean>
    private lateinit var localExcludedBlockchains: Map<String, Boolean>

    override val excludedBlockchainsIds: Set<String>
        get() {
            if (!isInitialized) error("ExcludedBlockchainsManager is not initialized")

            return currentExcludedBlockchains
                .filterValues { it }
                .keys
        }

    override suspend fun init() {
        localTogglesStorage.populate(path = "configs/excluded_blockchains_config")

        val storedExcludedBlockchainsIds = appPreferencesStore.getObjectMapSync<Boolean>(
            key = PreferencesKeys.EXCLUDED_BLOCKCHAINS_KEY,
        )

        localExcludedBlockchains = localTogglesStorage.toggles
            .associateToggles(currentVersion = versionProvider.get().orEmpty())
            .mapValues { (_, isIncluded) -> !isIncluded }

        currentExcludedBlockchains = (localExcludedBlockchains.keys + storedExcludedBlockchainsIds.keys)
            .fold(mutableMapOf()) { acc, blockchainId ->
                val isExcluded = storedExcludedBlockchainsIds[blockchainId] ?: localExcludedBlockchains[blockchainId]

                requireNotNull(isExcluded) {
                    "Unable to find $blockchainId in local or stored excluded blockchains"
                }

                acc[blockchainId] = isExcluded
                acc
            }

        isInitialized = true
    }

    override suspend fun excludeBlockchain(mainnetId: String, isExcluded: Boolean) {
        currentExcludedBlockchains[mainnetId] = isExcluded

        storeCurrent()
    }

    override fun isMatchLocalConfig(): Boolean {
        return currentExcludedBlockchains == localExcludedBlockchains
    }

    override suspend fun recoverLocalConfig() {
        currentExcludedBlockchains = localExcludedBlockchains.toMutableMap()

        storeCurrent()
    }

    private suspend fun storeCurrent() {
        appPreferencesStore.storeObjectMap(
            key = PreferencesKeys.EXCLUDED_BLOCKCHAINS_KEY,
            value = currentExcludedBlockchains,
        )
    }
}