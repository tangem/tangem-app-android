package com.tangem.blockchainsdk.providers

import androidx.datastore.core.DataStore
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchainsdk.BlockchainProvidersResponse
import com.tangem.blockchainsdk.converters.BlockchainProviderTypesConverter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Implementation of [BlockchainProvidersTypesManager] in DEV environment
 *
 * @property prodBlockchainProvidersTypesManager prod manager
 * @property blockchainProviderTypesStore        blockchain provider types store
 * @property changedBlockchainProvidersStore     changed blockchain providers store
 *
[REDACTED_AUTHOR]
 */
internal class DevBlockchainProvidersTypesManager(
    private val prodBlockchainProvidersTypesManager: ProdBlockchainProvidersTypesManager,
    private val blockchainProviderTypesStore: BlockchainProviderTypesStore,
    private val changedBlockchainProvidersStore: DataStore<BlockchainProvidersResponse>,
) : MutableBlockchainProvidersTypesManager {

    override fun get(): Flow<BlockchainProviderTypes> = changedBlockchainProvidersStore.data
        .map(BlockchainProviderTypesConverter::convert)

    override suspend fun update() {
        prodBlockchainProvidersTypesManager.update()

        val initial = blockchainProviderTypesStore.get().value

        val changed = changedBlockchainProvidersStore.data.firstOrNull().orEmpty()

        if (changed.isEmpty()) {
            Timber.i("Initialize ChangedBlockchainProvidersStore")
            changedBlockchainProvidersStore.updateData {
                BlockchainProviderTypesConverter.convertBack(initial)
            }
        }
    }

    override suspend fun recoverInitialState() {
        val initial = blockchainProviderTypesStore.get().value

        changedBlockchainProvidersStore.updateData {
            BlockchainProviderTypesConverter.convertBack(initial)
        }
    }

    override suspend fun update(blockchain: Blockchain, providers: List<ProviderType>) {
        changedBlockchainProvidersStore.updateData {
            val providerModels = BlockchainProviderTypesConverter.convertBack(value = mapOf(blockchain to providers))

            it + providerModels
        }
    }

    override suspend fun isMatchWithMerged(): Boolean {
        val initial = blockchainProviderTypesStore.get().value
        val changed = changedBlockchainProvidersStore.data.firstOrNull().orEmpty()

        return initial == BlockchainProviderTypesConverter.convert(changed)
    }
}