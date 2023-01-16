package com.tangem.tap.domain.configurable.config

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.tangem.tap.common.AssetReader
import com.tangem.tap.domain.configurable.Loader
import timber.log.Timber

/**
 * Created by Anton Zhilenkov on 16/02/2021.
 */
class FeaturesLocalLoader(
    private val assetReader: AssetReader,
    private val moshi: Moshi,
) : Loader<ConfigModel> {

    override fun load(onComplete: (ConfigModel) -> Unit) {
        val config = try {
            val featureAdapter: JsonAdapter<FeatureModel> = moshi.adapter(FeatureModel::class.java)
            val valuesAdapter: JsonAdapter<ConfigValueModel> = moshi.adapter(ConfigValueModel::class.java)

            val jsonFeatures = assetReader.readAssetAsString(Loader.featuresName)
            val jsonConfigValues = assetReader.readAssetAsString(Loader.configValuesName)

            ConfigModel(featureAdapter.fromJson(jsonFeatures), valuesAdapter.fromJson(jsonConfigValues))
        } catch (ex: Exception) {
            Timber.e(ex)
            ConfigModel.empty()
        }
        onComplete(config)
    }
}
