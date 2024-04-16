package com.tangem.blockchainsdk

import com.tangem.blockchain.common.AccountCreator
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchainsdk.converters.BlockchainProviderTypesConverter
import com.tangem.blockchainsdk.converters.BlockchainSDKConfigConverter
import com.tangem.blockchainsdk.storage.RuntimeStore
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.config.models.ConfigValueModel
import com.tangem.datasource.config.models.ProviderModel
import com.tangem.libs.blockchain_sdk.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

internal typealias BlockchainProvidersResponse = Map<String, List<ProviderModel>>
internal typealias BlockchainProviderTypes = Map<Blockchain, List<ProviderType>>

/**
 * Implementation of Blockchain SDK components factory
 *
 * @property assetLoader                  asset loader
 * @property configStore                  blockchain sdk config store
 * @property blockchainProviderTypesStore blockchain provider types store
 * @property accountCreator               account creator
 * @property blockchainDataStorage        blockchain data storage
 * @property blockchainSDKLogger          blockchain SDK logger
 *
* [REDACTED_AUTHOR]
 */
internal class DefaultBlockchainSDKFactory(
    private val assetLoader: AssetLoader,
    private val configStore: RuntimeStore<BlockchainSdkConfig>,
    private val blockchainProviderTypesStore: RuntimeStore<BlockchainProviderTypes>,
    private val accountCreator: AccountCreator,
    private val blockchainDataStorage: BlockchainDataStorage,
    private val blockchainSDKLogger: BlockchainSDKLogger,
) : BlockchainSDKFactory {

    override val walletManagerFactory: Flow<WalletManagerFactory> by lazy(::createWalletManagerFactory)

    override suspend fun init() {
        coroutineScope {
            updateBlockchainSDKConfig()
            updateBlockchainProviderTypes()
        }
    }

    override suspend fun getWalletManagerFactorySync(): WalletManagerFactory? = walletManagerFactory.firstOrNull()

    private fun createWalletManagerFactory(): Flow<WalletManagerFactory> {
        return combine(
            flow = configStore.get(),
            flow2 = blockchainProviderTypesStore.get(),
        ) { config, blockchainProviderTypes ->
            WalletManagerFactory(
                config = config,
                blockchainProviderTypes = blockchainProviderTypes,
                accountCreator = accountCreator,
                blockchainDataStorage = blockchainDataStorage,
                loggers = listOf(blockchainSDKLogger),
            )
        }
    }

    private fun CoroutineScope.updateBlockchainSDKConfig() {
        launch {
            val config = assetLoader.load<ConfigValueModel>(fileName = CONFIG_FILE_NAME) ?: return@launch

            configStore.store(
                value = BlockchainSDKConfigConverter.convert(value = config),
            )
        }
    }

    private fun CoroutineScope.updateBlockchainProviderTypes() {
        launch {
            val providerTypes = assetLoader.load<BlockchainProvidersResponse>(fileName = PROVIDER_TYPES_FILE_NAME)
                ?: return@launch

            blockchainProviderTypesStore.store(
                value = BlockchainProviderTypesConverter.convert(providerTypes),
            )
        }
    }

    private companion object {
        const val CONFIG_FILE_NAME = "tangem-app-config/config_${BuildConfig.ENVIRONMENT}"
        const val PROVIDER_TYPES_FILE_NAME = "tangem-app-config/providers_order"
    }
}
