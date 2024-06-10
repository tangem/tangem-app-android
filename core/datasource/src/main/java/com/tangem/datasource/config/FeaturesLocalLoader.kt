package com.tangem.datasource.config

import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.config.models.ConfigModel
import com.tangem.datasource.config.models.ConfigValueModel
import com.tangem.datasource.config.models.FeatureModel

/**
[REDACTED_AUTHOR]
 */
class FeaturesLocalLoader(
    private val assetLoader: AssetLoader,
    buildEnvironment: String,
) : Loader<ConfigModel> {

    private val featuresName = "features_$buildEnvironment"
    private val configValuesName = "tangem-app-config/config_$buildEnvironment"

    override suspend fun load(onComplete: (ConfigModel) -> Unit) {
        ConfigModel(
            features = assetLoader.load<FeatureModel>(fileName = featuresName),
            configValues = assetLoader.load<ConfigValueModel>(fileName = configValuesName),
        ).also(onComplete)
    }
}