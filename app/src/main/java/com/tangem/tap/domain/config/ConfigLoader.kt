package com.tangem.tap.domain.config

import android.content.Context
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.squareup.moshi.JsonAdapter
import com.tangem.wallet.BuildConfig
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
interface ConfigLoader {
    fun loadConfig(onComplete: (Config) -> Unit)
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

class LocalLoader(
        private val context: Context,
        private val nameResolver: NameResolver,
        private val adapter: JsonAdapter<ConfigModel>
) : ConfigLoader {

    override fun loadConfig(onComplete: (Config) -> Unit) {
        val config = try {
            val jsonFeatures = readAssetAsString(nameResolver.getFeaturesName())
            var configModel = adapter.fromJson(jsonFeatures)
            val features = configModel?.toFeatures(ConfigType.Local) ?: mutableMapOf()

            val jsonConfigValues = readAssetAsString(nameResolver.getConfigValuesName())
            configModel = adapter.fromJson(jsonConfigValues)
            val configValues = configModel?.toConfigValues() ?: mutableMapOf()
            Config(features, configValues)
        } catch (ex: Exception) {
            Timber.e(ex)
            Config.empty()
        }
        onComplete(config)
    }

    private fun readAssetAsString(fileName: String): String {
        return context.assets.open("$fileName.json").bufferedReader().readText()
    }
}

class RemoteLoader(
        private val nameResolver: NameResolver,
        private val adapter: JsonAdapter<ConfigModel>
) : ConfigLoader {

    override fun loadConfig(onComplete: (Config) -> Unit) {
        val emptyConfig = Config.empty()
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            if (it.isSuccessful) {
                val config = remoteConfig.getValue(nameResolver.getFeaturesName())
                val jsonConfig = config.asString()
                if (jsonConfig.isEmpty()) {
                    onComplete(emptyConfig)
                    return@addOnCompleteListener
                }
                val configModel = adapter.fromJson(jsonConfig)
                val features = configModel?.toFeatures(ConfigType.Remote) ?: mutableMapOf()
                val configValues = configModel?.toConfigValues() ?: mutableMapOf()
                onComplete(Config(features, configValues))
            } else {
                onComplete(emptyConfig)
            }
        }.addOnFailureListener {
            Timber.e(it)
            onComplete(emptyConfig)
        }
    }
}