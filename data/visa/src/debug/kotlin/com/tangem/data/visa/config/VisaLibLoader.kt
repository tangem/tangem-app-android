package com.tangem.data.visa.config

import com.squareup.moshi.Moshi
import com.tangem.data.visa.BuildConfig
import com.tangem.data.visa.utils.VisaConstants
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.lib.visa.VisaContractInfoProvider
import com.tangem.lib.visa.api.VisaApi
import com.tangem.lib.visa.api.VisaApiBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

internal class VisaLibLoader @Inject constructor(
    private val assetLoader: AssetLoader,
    @NetworkMoshi private val moshi: Moshi,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    private val createMutex = Mutex()

    private var config: VisaConfig? = null

    private var provider: VisaContractInfoProvider? = null
    private var api: VisaApi? = null

    suspend fun getOrCreateProvider(): VisaContractInfoProvider = provider ?: createProvider()

    suspend fun getOrCreateApi(): VisaApi = api ?: createApi()

    private suspend fun createProvider(): VisaContractInfoProvider = createMutex.withLock {
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

    private suspend fun createApi(): VisaApi = createMutex.withLock {
        val config = getOrLoadConfig()

        api = VisaApiBuilder(
            useDevApi = VisaConstants.USE_TEST_ENV,
            isNetworkLoggingEnabled = BuildConfig.LOG_ENABLED,
            moshi = moshi,
            headers = mapOf(
                X_ASN_HEADER_NAME to config.header.xAsn,
            ),
        ).build()

        return requireNotNull(api) {
            "Visa API is not created"
        }
    }

    private suspend fun getOrLoadConfig(): VisaConfig {
        config = assetLoader.load<VisaConfig>(VISA_CONFIG_FILE_NAME)

        return requireNotNull(config) {
            "Visa config is not found"
        }
    }

    companion object {
        private const val VISA_CONFIG_FILE_NAME = "tangem-app-config/visa_config"
        private const val X_ASN_HEADER_NAME = "x-asn"
    }
}
