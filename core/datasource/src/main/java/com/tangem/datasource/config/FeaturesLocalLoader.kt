package com.tangem.datasource.config

import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.config.models.ConfigModel
import com.tangem.datasource.config.models.ConfigValueModel

/**
[REDACTED_AUTHOR]
 */
class FeaturesLocalLoader(
    private val assetLoader: AssetLoader,
    buildEnvironment: String,
) : Loader<ConfigModel> {

    private val configValuesName = "tangem-app-config/config_$buildEnvironment"

    override suspend fun load(onComplete: (ConfigModel) -> Unit) {
        ConfigModel(
            configValues = assetLoader.load<ConfigValueModel>(fileName = configValuesName),
        ).also(onComplete)
    }
}