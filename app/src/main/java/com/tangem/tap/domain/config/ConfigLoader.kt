package com.tangem.tap.domain.config

import android.content.Context
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.wallet.BuildConfig
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
interface ConfigLoader {
    fun loadConfig(onComplete: (ConfigModel) -> Unit)
}


interface NameResolver {
    fun getFeaturesName(): String
    fun getConfigValuesName(): String
}

class ConfigNameResolver {
    companion object {
        fun get(): NameResolver = if (BuildConfig.DEBUG) dev() else prod()

        private fun dev(): NameResolver {
            return object : NameResolver {
                override fun getFeaturesName(): String = "dev_features"
                override fun getConfigValuesName(): String = "dev_config_values"
            }
        }

        private fun prod(): NameResolver {
            return object : NameResolver {
                override fun getFeaturesName(): String = "prod_features"
                override fun getConfigValuesName(): String = "prod_config_values"

            }
        }
    }
}

class KeyValueModel<T>(val name: String, value: T?)

class LocalLoader(
        private val context: Context,
        private val nameResolver: NameResolver,
        private val moshi: Moshi
) : ConfigLoader {

    override fun loadConfig(onComplete: (ConfigModel) -> Unit) {
        val config = try {
            val featureType = Types.newParameterizedType(List::class.java, FeatureModel::class.java)
            val featureAdapter: JsonAdapter<List<FeatureModel>> = moshi.adapter(featureType)
            val valuesType = Types.newParameterizedType(List::class.java, ConfigValueModel::class.java)
            val valuesAdapter: JsonAdapter<List<ConfigValueModel>> = moshi.adapter(valuesType)

            val jsonFeatures = readAssetAsString(nameResolver.getFeaturesName())
            val jsonConfigValues = readAssetAsString(nameResolver.getConfigValuesName())

            ConfigModel(featureAdapter.fromJson(jsonFeatures) ?: listOf(),
                    valuesAdapter.fromJson(jsonConfigValues) ?: listOf())
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
        private val nameResolver: NameResolver,
        private val moshi: Moshi
) : ConfigLoader {

    override fun loadConfig(onComplete: (ConfigModel) -> Unit) {
        val emptyConfig = ConfigModel.empty()
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            if (it.isSuccessful) {
                val config = remoteConfig.getValue(nameResolver.getFeaturesName())
                val jsonConfig = config.asString()
                if (jsonConfig.isEmpty()) {
                    onComplete(emptyConfig)
                    return@addOnCompleteListener
                }
                val featureType = Types.newParameterizedType(List::class.java, FeatureModel::class.java)
                val featureAdapter: JsonAdapter<List<FeatureModel>> = moshi.adapter(featureType)
                onComplete(ConfigModel(featureAdapter.fromJson(jsonConfig) ?: listOf(), listOf()))
            } else {
                onComplete(emptyConfig)
            }
        }.addOnFailureListener {
            Timber.e(it)
            onComplete(emptyConfig)
        }
    }
}