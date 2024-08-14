package com.tangem.blockchainsdk

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchainsdk.converters.BlockchainProviderTypesConverter
import com.tangem.blockchainsdk.converters.BlockchainSDKConfigConverter
import com.tangem.blockchainsdk.loader.BlockchainProvidersResponseLoader
import com.tangem.blockchainsdk.store.RuntimeStore
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.config.models.ConfigValueModel
import com.tangem.datasource.config.models.ProviderModel
import com.tangem.libs.blockchain_sdk.BuildConfig
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
 * @property assetLoader                       asset loader
 * @property blockchainProvidersResponseLoader blockchain providers response loader
 * @property configStore                       blockchain sdk config store
 * @property blockchainProviderTypesStore      blockchain provider types store
 * @property walletManagerFactoryCreator       wallet manager factory creator
 *
* [REDACTED_AUTHOR]
 */
internal class DefaultBlockchainSDKFactory(
    private val assetLoader: AssetLoader,
    private val blockchainProvidersResponseLoader: BlockchainProvidersResponseLoader,
    private val configStore: RuntimeStore<BlockchainSdkConfig>,
    private val blockchainProviderTypesStore: RuntimeStore<BlockchainProviderTypes>,
    private val walletManagerFactoryCreator: WalletManagerFactoryCreator,
    dispatchers: CoroutineDispatcherProvider,
) : BlockchainSDKFactory {

    private val mainScope = CoroutineScope(dispatchers.main)

    private val walletManagerFactory: Flow<WalletManagerFactory?> = createWalletManagerFactory()

    override suspend fun init() {
        coroutineScope {
            updateBlockchainSDKConfig()
            updateBlockchainProviderTypes()
        }
    }

    override suspend fun getWalletManagerFactorySync(): WalletManagerFactory? = walletManagerFactory.firstOrNull()

    private fun createWalletManagerFactory(): Flow<WalletManagerFactory?> {
        return combine(
            flow = configStore.get(),
            flow2 = blockchainProviderTypesStore.get(),
            // flow3 = subscribe on feature toggles changes, TODO: https://tangem.atlassian.net/browse/AND-7067
            transform = walletManagerFactoryCreator::create,
        )
            .stateIn(scope = mainScope, started = SharingStarted.Eagerly, initialValue = null)
    }

    private fun CoroutineScope.updateBlockchainSDKConfig() {
        launch {
            val config = assetLoader.load<ConfigValueModel>(fileName = CONFIG_FILE_NAME)

            if (config == null) {
                Timber.e("Error loading BlockchainSDKConfig")
                return@launch
            }

            Timber.i("Update BlockchainSDKConfig")

            configStore.store(
                value = BlockchainSDKConfigConverter.convert(value = config),
            )
        }
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

    private companion object {
        const val CONFIG_FILE_NAME = "tangem-app-config/config_${BuildConfig.ENVIRONMENT}"
    }
}
