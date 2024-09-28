package com.tangem.blockchainsdk

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchainsdk.converters.BlockchainProviderTypesConverter
import com.tangem.blockchainsdk.loader.BlockchainProvidersResponseLoader
import com.tangem.blockchainsdk.store.RuntimeStore
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.datasource.local.config.environment.models.ProviderModel
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

internal typealias BlockchainProvidersResponse = Map<String, List<ProviderModel>>
internal typealias BlockchainProviderTypes = Map<Blockchain, List<ProviderType>>

/**
 * Implementation of Blockchain SDK components factory
 *
 * @property blockchainProvidersResponseLoader blockchain providers response loader
 * @property environmentConfigStorage          environment config storage
 * @property blockchainProviderTypesStore      blockchain provider types store
 * @property walletManagerFactoryCreator       wallet manager factory creator
 *
* [REDACTED_AUTHOR]
 */
internal class DefaultBlockchainSDKFactory(
    private val blockchainProvidersResponseLoader: BlockchainProvidersResponseLoader,
    private val environmentConfigStorage: EnvironmentConfigStorage,
    private val blockchainProviderTypesStore: RuntimeStore<BlockchainProviderTypes>,
    private val walletManagerFactoryCreator: WalletManagerFactoryCreator,
    dispatchers: CoroutineDispatcherProvider,
) : BlockchainSDKFactory {

    private val mainScope = CoroutineScope(dispatchers.main)

    private val walletManagerFactory: Flow<WalletManagerFactory?> = createWalletManagerFactory()
// [REDACTED_TODO_COMMENT]
    // private val walletManagerFactory: Flow<WalletManagerFactory?> by lazy(LazyThreadSafetyMode.NONE) {
    // createWalletManagerFactory()
    // }

    override suspend fun init() {
        coroutineScope {
            updateBlockchainProviderTypes()
        }
    }

    override suspend fun getWalletManagerFactorySync(): WalletManagerFactory? = walletManagerFactory.firstOrNull()

    private fun createWalletManagerFactory(): Flow<WalletManagerFactory?> {
        return combine(
            flow = environmentConfigStorage.getConfig().map { it.blockchainSdkConfig },
            flow2 = blockchainProviderTypesStore.get(),
            // flow3 = subscribe on feature toggles changes, TODO: https://tangem.atlassian.net/browse/AND-7067
            transform = walletManagerFactoryCreator::create,
        )
            .stateIn(scope = mainScope, started = SharingStarted.Eagerly, initialValue = null)
    }

    private fun CoroutineScope.updateBlockchainProviderTypes() {
        launch {
            val response = blockchainProvidersResponseLoader.load()

            if (response == null) {
                Timber.e("Error loading BlockchainProviderTypes")
                return@launch
            }

            Timber.i("Update BlockchainProviderTypes")

            blockchainProviderTypesStore.store(
                value = BlockchainProviderTypesConverter.convert(response),
            )
        }
    }
}
