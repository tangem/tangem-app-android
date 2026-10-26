package com.tangem.data.visa.config

import com.tangem.data.visa.BuildConfig
import com.tangem.data.visa.utils.VisaConstants
import com.tangem.core.local.assets.AssetLoader
import com.tangem.lib.visa.VisaContractInfoProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

internal class VisaLibLoader @Inject constructor(
    private val assetLoader: AssetLoader,
    private val dispatchers: CoroutineDispatcherProvider,
) {
    private val createMutex = Mutex()
    private val createMutex2 = Mutex()

    @Volatile
    private var config: VisaConfig? = null

    @Volatile
    private var provider: VisaContractInfoProvider? = null

    suspend fun getOrCreateConfig(): VisaConfig = config ?: getOrLoadConfig()

    suspend fun getOrCreateProvider(): VisaContractInfoProvider = provider ?: createProvider()

    private suspend fun createProvider(): VisaContractInfoProvider = createMutex.withLock {
        provider?.let { return@withLock it }

        val config = getOrLoadConfig()

        VisaContractInfoProvider.Builder(
            isTestnetRpcEnabled = VisaConstants.USE_TEST_ENV,
            bridgeProcessorAddress = if (VisaConstants.USE_TEST_ENV) {
                config.testnet.bridgeProcessor
            } else {
                config.mainnet.bridgeProcessor
            },
            paymentAccountRegistryAddress = if (VisaConstants.USE_TEST_ENV) {
                config.testnet.paymentAccountRegistry
            } else {
                config.mainnet.paymentAccountRegistry
            },
            isNetworkLoggingEnabled = BuildConfig.LOG_ENABLED,
            dispatchers = dispatchers,
        ).build()
            .also { provider = it }
    }

    private suspend fun getOrLoadConfig(): VisaConfig = createMutex2.withLock {
        config?.let { return@withLock it }

        val loadedConfig = requireNotNull(
            assetLoader.load(fileName = VISA_CONFIG_FILE_NAME, deserializer = VisaConfig.serializer()),
        ) { "Visa config is not loaded" }
        config = loadedConfig
        loadedConfig
    }

    companion object {
        private const val VISA_CONFIG_FILE_NAME = "tangem-app-config/visa_config"
    }
}