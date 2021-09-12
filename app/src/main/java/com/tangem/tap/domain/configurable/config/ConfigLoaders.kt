package com.tangem.tap.domain.configurable.config

import android.content.Context
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.common.extensions.readAssetAsString
import com.tangem.tap.domain.configurable.Loader
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
class FeaturesLocalLoader(
        private val context: Context,
        private val moshi: Moshi,
) : Loader<ConfigModel> {

    override fun load(onComplete: (ConfigModel) -> Unit) {
        val config = try {
            val featureAdapter: JsonAdapter<FeatureModel> = moshi.adapter(FeatureModel::class.java)
            val valuesAdapter: JsonAdapter<ConfigValueModel> = moshi.adapter(ConfigValueModel::class.java)

            val jsonFeatures = context.readAssetAsString(Loader.featuresName)
            val jsonConfigValues = context.readAssetAsString(Loader.configValuesName)

            ConfigModel(featureAdapter.fromJson(jsonFeatures), valuesAdapter.fromJson(jsonConfigValues))
        } catch (ex: Exception) {
            Timber.e(ex)
            ConfigModel.empty()
        }
        onComplete(config)
    }
}

class FeaturesRemoteLoader(
        private val moshi: Moshi,
) : Loader<ConfigModel> {

    override fun load(onComplete: (ConfigModel) -> Unit) {
        val emptyConfig = ConfigModel.empty()
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            if (it.isSuccessful) {
                val config = remoteConfig.getValue(Loader.featuresName)
                val jsonConfig = config.asString()
                if (jsonConfig.isEmpty()) {
                    onComplete(emptyConfig)
                    return@addOnCompleteListener
                }
                val featureAdapter: JsonAdapter<FeatureModel> = moshi.adapter(FeatureModel::class.java)
                onComplete(ConfigModel(featureAdapter.fromJson(jsonConfig), null))
            } else {
                onComplete(emptyConfig)
            }
        }.addOnFailureListener {
            FirebaseAnalyticsHandler.logException("remote_config_error.features", it)
            onComplete(emptyConfig)
        }
    }
}