package com.tangem.blockchainsdk

import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchainsdk.providers.BlockchainProvidersTypesManager
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.datasource.local.config.providers.models.ProviderModel
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal typealias BlockchainProvidersResponse = Map<String, List<ProviderModel>>

/**
 * Implementation of Blockchain SDK components factory
 *
 * @property blockchainProvidersTypesManager blockchain providers types manager
 * @property environmentConfigStorage        environment config storage
 * @property walletManagerFactoryCreator     wallet manager factory creator
 * @param dispatchers                     coroutine dispatchers provider
 *
[REDACTED_AUTHOR]
 */
internal class DefaultBlockchainSDKFactory(
    private val blockchainProvidersTypesManager: BlockchainProvidersTypesManager,
    private val environmentConfigStorage: EnvironmentConfigStorage,
    private val walletManagerFactoryCreator: WalletManagerFactoryCreator,
    dispatchers: CoroutineDispatcherProvider,
) : BlockchainSDKFactory {

    private val mainScope = CoroutineScope(dispatchers.main)

    private val walletManagerFactory: Flow<WalletManagerFactory?> = createWalletManagerFactory()

    override suspend fun init() {
        coroutineScope {
            launch { blockchainProvidersTypesManager.update() }
        }
    }

    override suspend fun getWalletManagerFactorySync(): WalletManagerFactory? = walletManagerFactory.firstOrNull()

    private fun createWalletManagerFactory(): Flow<WalletManagerFactory?> {
        return combine(
            flow = environmentConfigStorage.getConfig().map { it.blockchainSdkConfig },
            flow2 = blockchainProvidersTypesManager.get(),
            // flow3 = subscribe on feature toggles changes, TODO: [REDACTED_JIRA]
            transform = walletManagerFactoryCreator::create,
        )
            // don't use Lazily because some features (WC) require initialized factory on app started
            .stateIn(scope = mainScope, started = SharingStarted.Eagerly, initialValue = null)
    }
}