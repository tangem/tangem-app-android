package com.tangem.datasource.config

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.tangem.datasource.asset.reader.AssetReader
import com.tangem.datasource.config.models.ConfigModel
import com.tangem.datasource.config.models.ConfigValueModel
import com.tangem.datasource.config.models.FeatureModel
import timber.log.Timber

/**
* [REDACTED_AUTHOR]
 */
@Deprecated(message = "Use AssetReader instead")
class FeaturesLocalLoader(
    private val assetReader: AssetReader,
    private val moshi: Moshi,
    buildEnvironment: String,
) : Loader<ConfigModel> {

    private val featuresName = "features_$buildEnvironment"
    private val configValuesName = "tangem-app-config/config_$buildEnvironment"

    override fun load(onComplete: (ConfigModel) -> Unit) {
        val config = try {
            val featureAdapter: JsonAdapter<FeatureModel> = moshi.adapter(FeatureModel::class.java)
            val valuesAdapter: JsonAdapter<ConfigValueModel> = moshi.adapter(ConfigValueModel::class.java)

            val jsonFeatures = assetReader.readJson(featuresName)
            val jsonConfigValues = assetReader.readJson(configValuesName)

            ConfigModel(featureAdapter.fromJson(jsonFeatures), valuesAdapter.fromJson(jsonConfigValues))
        } catch (ex: Exception) {
            Timber.e(ex)
            ConfigModel.empty()
        }
        onComplete(config)
    }
}
