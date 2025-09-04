package com.tangem.core.configtoggle.blockchain.impl

import com.tangem.core.configtoggle.ExcludedBlockchainToggles
import com.tangem.core.configtoggle.blockchain.MutableExcludedBlockchainsManager
import com.tangem.core.configtoggle.storage.LocalTogglesStorage
import com.tangem.core.configtoggle.utils.defineTogglesAvailability
import com.tangem.core.configtoggle.utils.toTableString
import com.tangem.core.configtoggle.version.VersionProvider
import kotlinx.coroutines.runBlocking
import kotlin.properties.Delegates

/**
 * [MutableExcludedBlockchainsManager] implementation in dev or mocked build
 *
 * @property versionProvider     application version provider
 * @property localTogglesStorage local storage for blockchain toggles
 */
internal class DevExcludedBlockchainsManager(
    private val versionProvider: VersionProvider,
    private val localTogglesStorage: LocalTogglesStorage,
) : MutableExcludedBlockchainsManager {

    private val fileBlockchainToggles: Map<String, Boolean> = getFileBlockchainToggles()
    private var blockchainTogglesMap: MutableMap<String, Boolean> by Delegates.notNull()

    override val excludedBlockchainsIds: Set<String>
        get() = blockchainTogglesMap.filterValues { !it }.keys

    init {
        val savedExcludedBlockchains = runBlocking { localTogglesStorage.getSyncOrEmpty() }

        blockchainTogglesMap = fileBlockchainToggles
            .mapValues { (blockchainId, isEnabled) ->
                savedExcludedBlockchains[blockchainId] ?: isEnabled
            }
            .toMutableMap()
    }

    override suspend fun excludeBlockchain(mainnetId: String, isExcluded: Boolean) {
        blockchainTogglesMap[mainnetId] = isExcluded

        localTogglesStorage.store(blockchainTogglesMap)
    }

    override fun isMatchLocalConfig(): Boolean {
        return blockchainTogglesMap == fileBlockchainToggles
    }

    override suspend fun recoverLocalConfig() {
        blockchainTogglesMap = fileBlockchainToggles.toMutableMap()

        localTogglesStorage.store(blockchainTogglesMap)
    }

    override fun toString(): String {
        return blockchainTogglesMap
            .filterKeys { it.isNotEmpty() }
            .toTableString(tableName = this@DevExcludedBlockchainsManager::class.java.simpleName)
    }

    private fun getFileBlockchainToggles(): Map<String, Boolean> {
        val appVersion = versionProvider.get()

        return ExcludedBlockchainToggles.values.defineTogglesAvailability(appVersion = appVersion)
    }
}