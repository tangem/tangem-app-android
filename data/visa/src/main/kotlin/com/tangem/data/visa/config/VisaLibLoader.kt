package com.tangem.data.visa.config

import com.tangem.data.visa.BuildConfig
import com.tangem.data.visa.utils.VisaConstants
import com.tangem.datasource.asset.loader.AssetLoader
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
        if (provider != null) return@withLock requireNotNull(provider)

        val config = getOrLoadConfig()

        provider = VisaContractInfoProvider.Builder(
            useTestnetRpc = VisaConstants.USE_TEST_ENV,
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

        return requireNotNull(provider) {
            "Visa provider is not created"
        }
    }

    private suspend fun getOrLoadConfig(): VisaConfig = createMutex2.withLock {
        if (config != null) return@withLock requireNotNull(config)

        config = assetLoader.load<VisaConfig>(VISA_CONFIG_FILE_NAME)

        return@withLock requireNotNull(config) {
            "Visa config is not loaded"
        }
    }

    companion object {
        private const val VISA_CONFIG_FILE_NAME = "tangem-app-config/visa_config"
    }
}