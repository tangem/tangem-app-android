package com.tangem.datasource.config

import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.config.models.ConfigModel
import com.tangem.datasource.config.models.ConfigValueModel

/**
 * Created by Anton Zhilenkov on 16/02/2021.
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
