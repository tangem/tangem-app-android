package com.tangem.tap.domain.config

import android.content.Context
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.wallet.BuildConfig
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
interface ConfigLoader {
    fun loadConfig(onComplete: (ConfigModel) -> Unit)

    companion object {
        const val featuresName = "features_${BuildConfig.CONFIG_ENVIRONMENT}"
        const val configValuesName = "config_${BuildConfig.CONFIG_ENVIRONMENT}"
    }
}


class LocalLoader(
        private val context: Context,
        private val moshi: Moshi
) : ConfigLoader {

    override fun loadConfig(onComplete: (ConfigModel) -> Unit) {
        val config = try {
            val featureAdapter: JsonAdapter<FeatureModel> = moshi.adapter(FeatureModel::class.java)
            val valuesAdapter: JsonAdapter<ConfigValueModel> = moshi.adapter(ConfigValueModel::class.java)

            val jsonFeatures = readAssetAsString(ConfigLoader.featuresName)
            val jsonConfigValues = readAssetAsString(ConfigLoader.configValuesName)

            ConfigModel(featureAdapter.fromJson(jsonFeatures), valuesAdapter.fromJson(jsonConfigValues))
        } catch (ex: Exception) {
            Timber.e(ex)
            ConfigModel.empty()
        }
        onComplete(config)
    }

    private fun readAssetAsString(fileName: String): String {
        return context.assets.open("$fileName.json").bufferedReader().readText()
    }
}

class RemoteLoader(
        private val moshi: Moshi
) : ConfigLoader {

    override fun loadConfig(onComplete: (ConfigModel) -> Unit) {
        val emptyConfig = ConfigModel.empty()
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            if (it.isSuccessful) {
                val config = remoteConfig.getValue(ConfigLoader.featuresName)
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
            FirebaseAnalyticsHandler.logException("remote_config_error", it)
            onComplete(emptyConfig)
        }
    }
}