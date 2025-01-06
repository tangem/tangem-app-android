package com.tangem.blockchainsdk.providers

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.ProviderType
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.properties.Delegates.notNull

/**
 * Implementation of [BlockchainProvidersTypesManager] in DEV environment
 *
 * @property prodBlockchainProvidersTypesManager prod manager
 * @property blockchainProviderTypesStore        blockchain provider types store
 *
[REDACTED_AUTHOR]
 */
@Singleton
internal class DevBlockchainProvidersTypesManager @Inject constructor(
    private val prodBlockchainProvidersTypesManager: ProdBlockchainProvidersTypesManager,
    private val blockchainProviderTypesStore: BlockchainProviderTypesStore,
) : BlockchainProvidersTypesManager by prodBlockchainProvidersTypesManager,
    MutableBlockchainProvidersTypesManager {

    private var primaryResponse: BlockchainProviderTypes by notNull()

    override suspend fun update() {
        prodBlockchainProvidersTypesManager.update()

        primaryResponse = blockchainProviderTypesStore.get().value
    }

    override suspend fun update(blockchain: Blockchain, providers: List<ProviderType>) {
        val currentTypes = blockchainProviderTypesStore.get().value

        val updatedTypes = currentTypes + (blockchain to providers)

        blockchainProviderTypesStore.store(value = updatedTypes)
    }

    override suspend fun isMatchWithMerged(): Boolean {
        val current = blockchainProviderTypesStore.get().value

        return current == primaryResponse
    }
}