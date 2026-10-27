package com.tangem.data.visa.config

import com.tangem.core.local.assets.AssetLoader
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

internal class VisaLibLoader @Inject constructor(
    private val assetLoader: AssetLoader,
) {
    private val configMutex = Mutex()

    @Volatile
    private var config: VisaConfig? = null

    suspend fun getOrCreateConfig(): VisaConfig = config ?: getOrLoadConfig()

    private suspend fun getOrLoadConfig(): VisaConfig = configMutex.withLock {
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