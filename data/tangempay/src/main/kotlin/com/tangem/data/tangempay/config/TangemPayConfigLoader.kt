package com.tangem.data.tangempay.config

import com.tangem.core.local.assets.AssetLoader
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

internal class TangemPayConfigLoader @Inject constructor(
    private val assetLoader: AssetLoader,
) {
    private val configMutex = Mutex()

    @Volatile
    private var config: TangemPayConfig? = null

    suspend fun getOrCreateConfig(): TangemPayConfig = config ?: getOrLoadConfig()

    private suspend fun getOrLoadConfig(): TangemPayConfig = configMutex.withLock {
        config?.let { return@withLock it }

        val loadedConfig = requireNotNull(
            assetLoader.load(fileName = TANGEM_PAY_CONFIG_FILE_NAME, deserializer = TangemPayConfig.serializer()),
        ) { "Tangem Pay config is not loaded" }
        config = loadedConfig
        loadedConfig
    }

    companion object {
        private const val TANGEM_PAY_CONFIG_FILE_NAME = "tangem-app-config/visa_config"
    }
}