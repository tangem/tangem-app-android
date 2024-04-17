package com.tangem.datasource.asset.loader

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapter
import com.tangem.datasource.asset.reader.AssetReader
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import timber.log.Timber
import javax.inject.Inject

/**
 * Asset file loader
 *
* [REDACTED_AUTHOR]
 */
class AssetLoader @Inject constructor(
    val assetReader: AssetReader,
    @NetworkMoshi val moshi: Moshi,
    val dispatchers: CoroutineDispatcherProvider,
) {

    /** Load content [Content] of asset file [fileName] */
    @OptIn(ExperimentalStdlibApi::class)
    suspend inline fun <reified Content> load(fileName: String): Content? {
        return runCatching(dispatchers.io) {
            val json = assetReader.readJson(fileName = fileName)

            moshi.adapter<Content>().fromJson(json)
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

    /** Load list [V] values of asset file [fileName] */
    suspend inline fun <reified V> loadList(fileName: String): List<V> {
        return runCatching(dispatchers.io) {
            val json = assetReader.readJson(fileName = fileName)

            val type = Types.newParameterizedType(List::class.java, V::class.java)
            val adapter = moshi.adapter<List<V>>(type)

            adapter.fromJson(json)
        }
            .fold(
                onSuccess = { parsedConfig ->
                    if (parsedConfig == null) Timber.e(IllegalStateException("Parsed config is null"))
                    parsedConfig.orEmpty()
                },
                onFailure = {
                    Timber.e(it, "Failed to load config from assets")
                    emptyList()
                },
            )
    }

    /** Load map [String] keys and [V] values of asset file [fileName] */
    suspend inline fun <reified V> loadMap(fileName: String): Map<String, V> {
        return runCatching(dispatchers.io) {
            val json = assetReader.readJson(fileName = fileName)

            val type = Types.newParameterizedType(Map::class.java, String::class.java, V::class.java)
            val adapter = moshi.adapter<Map<String, V>>(type)

            adapter.fromJson(json)
        }
            .fold(
                onSuccess = { parsedConfig ->
                    if (parsedConfig == null) Timber.e(IllegalStateException("Parsed config is null"))
                    parsedConfig.orEmpty()
                },
                onFailure = {
                    Timber.e(it, "Failed to load config from assets")
                    emptyMap()
                },
            )
    }
}
