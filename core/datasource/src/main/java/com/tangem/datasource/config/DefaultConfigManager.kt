package com.tangem.datasource.config

import com.tangem.datasource.BuildConfig
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.config.converter.EnvironmentConfigConverter
import com.tangem.datasource.config.models.Config
import com.tangem.datasource.config.models.ConfigValueModel
import com.tangem.datasource.local.datastore.RuntimeStateStore
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Default config manager
 *
 * @property assetLoader asset loader
 * @property configStore config store
 */
internal class DefaultConfigManager(
    private val assetLoader: AssetLoader,
    private val configStore: RuntimeStateStore<Config>,
) : ConfigManager {

    override suspend fun initialize(): Config {
        val configValueModel = assetLoader.load<ConfigValueModel>(fileName = CONFIG_FILE_NAME)
            ?: return configStore.get().value

        val config = EnvironmentConfigConverter.convert(value = configValueModel)
        configStore.store(value = config)

        Timber.i("Config [$CONFIG_FILE_NAME] loaded successfully")

        return config
    }

    override fun getConfig(): Flow<Config> = configStore.get()

    override fun getConfigSync(): Config = configStore.get().value

    private companion object {
        const val CONFIG_FILE_NAME = "tangem-app-config/config_${BuildConfig.ENVIRONMENT}"
    }
}