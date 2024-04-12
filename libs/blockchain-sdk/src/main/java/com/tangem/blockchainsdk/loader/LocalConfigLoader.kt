package com.tangem.blockchainsdk.loader

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.tangem.datasource.asset.reader.AssetReader
import com.tangem.datasource.config.models.ConfigValueModel
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import timber.log.Timber

/**
 * Local config loader
 *
 * @param buildEnvironment build environment that defines config file name
 * @param moshi            moshi
 * @property assetReader   asset reader
 * @property dispatchers   dispatchers
 */
internal class LocalConfigLoader(
    buildEnvironment: String,
    moshi: Moshi,
    private val assetReader: AssetReader,
    private val dispatchers: CoroutineDispatcherProvider,
) : ConfigLoader {

    private val configFileName = "tangem-app-config/config_$buildEnvironment"

    @OptIn(ExperimentalStdlibApi::class)
    private val adapter: JsonAdapter<ConfigValueModel> = moshi.adapter()

    @Deprecated(message = "Use AssetReader instead")
    override suspend fun load(): ConfigValueModel? {
        return runCatching(dispatchers.io) {
            val json = assetReader.readJson(fileName = configFileName)
            adapter.fromJson(json)
        }
            .fold(
                onSuccess = { parsedConfig ->
                    if (parsedConfig == null) Timber.e(IllegalStateException("Parsed config is null"))
                    parsedConfig
                },
                onFailure = {
                    Timber.e(it, "Failed to load config from assets")
                    null
                },
            )
    }
}
