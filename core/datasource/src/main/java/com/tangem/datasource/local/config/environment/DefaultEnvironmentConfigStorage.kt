package com.tangem.datasource.local.config.environment

import com.tangem.datasource.BuildConfig
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.local.config.environment.converter.EnvironmentConfigConverter
import com.tangem.datasource.local.config.environment.models.EnvironmentConfigModel
import com.tangem.datasource.local.datastore.RuntimeStateStore
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Default implementation for storing [EnvironmentConfig]
 *
 * @property assetLoader asset loader
 * @property environmentConfigStore config store
 */
internal class DefaultEnvironmentConfigStorage(
    private val assetLoader: AssetLoader,
    private val environmentConfigStore: RuntimeStateStore<EnvironmentConfig>,
) : EnvironmentConfigStorage {

    override suspend fun initialize(): EnvironmentConfig {
        val environmentConfigModel = assetLoader.load<EnvironmentConfigModel>(fileName = CONFIG_FILE_NAME)
            ?: return environmentConfigStore.get().value

        val config = EnvironmentConfigConverter.convert(value = environmentConfigModel)
        environmentConfigStore.store(value = config)

        Timber.i("Config [$CONFIG_FILE_NAME] loaded successfully")

        return config
    }

    override fun getConfig(): Flow<EnvironmentConfig> = environmentConfigStore.get()

    override fun getConfigSync(): EnvironmentConfig = environmentConfigStore.get().value

    private companion object {
        const val CONFIG_FILE_NAME = "tangem-app-config/config_${BuildConfig.ENVIRONMENT}"
    }
}