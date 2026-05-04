package com.tangem.datasource.asset.loader

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapter
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.datasource.asset.reader.AssetReader
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Asset file loader
 *
 * @property assetReader asset reader
 * @property moshi       moshi
 *
 * @see <a href = "https://www.notion.so/tangem/Assets-e045dd890413413faf34ce07ae47ff56">Documentation</a>
 *
[REDACTED_AUTHOR]
 */
@Singleton
class AssetLoader @Inject constructor(
    val assetReader: AssetReader,
    @NetworkMoshi val moshi: Moshi,
    val analyticsExceptionHandler: AnalyticsExceptionHandler,
    val dispatchers: CoroutineDispatcherProvider,
) {
    /** Load content [Content] of asset file [fileName] */
    @Suppress("SuspendFunSwallowedCancellation")
    @OptIn(ExperimentalStdlibApi::class)
    suspend inline fun <reified Content> load(fileName: String): Content? = withContext(dispatchers.io) {
        val json = runCatching { assetReader.read(fullFileName = "$fileName.json") }.getOrNull()

        runCatching {
            moshi.adapter<Content>().fromJson(json)
        }
            .fold(
                onSuccess = { parsedConfig ->
                    if (parsedConfig == null) {
                        sendException(
                            fileName = fileName,
                            isParsingSuccess = true,
                            json = json,
                        )

                        TangemLogger.e(
                            "Error",
                            IllegalStateException("Parsed config [$fileName] is null"),
                        )
                    }

                    parsedConfig
                },
                onFailure = { throwable ->
                    sendException(
                        fileName = fileName,
                        isParsingSuccess = false,
                        json = json,
                    )

                    TangemLogger.e("Failed to load config [$fileName] from assets", throwable)
                    null
                },
            )
    }

    /** Load list [V] values of asset file [fileName] */
    suspend inline fun <reified V> loadList(fileName: String): List<V> {
        val result = runCatching(dispatchers.io) {
            val json = assetReader.read(fullFileName = "$fileName.json")

            val type = Types.newParameterizedType(List::class.java, V::class.java)
            val adapter = moshi.adapter<List<V>>(type)

            adapter.fromJson(json)
        }
        return result.fold(
            onSuccess = { parsedConfig ->
                if (parsedConfig == null) {
                    TangemLogger.e(
                        "Error",
                        IllegalStateException("Parsed config [$fileName] is null"),
                    )
                }
                parsedConfig.orEmpty()
            },
            onFailure = { throwable ->
                TangemLogger.e("Failed to load config [$fileName] from assets", throwable)
                emptyList()
            },
        )
    }

    /** Load map [String] keys and [V] values of asset file [fileName] */
    suspend inline fun <reified V> loadMap(fileName: String): Map<String, V> {
        val result = runCatching(dispatchers.io) {
            val json = assetReader.read(fullFileName = "$fileName.json")

            val type = Types.newParameterizedType(Map::class.java, String::class.java, V::class.java)
            val adapter = moshi.adapter<Map<String, V>>(type)

            adapter.fromJson(json)
        }
        return result.fold(
            onSuccess = { parsedConfig ->
                if (parsedConfig == null) {
                    TangemLogger.e(
                        "Error",
                        IllegalStateException("Parsed config [$fileName] is null"),
                    )
                }
                parsedConfig.orEmpty()
            },
            onFailure = { throwable ->
                TangemLogger.e("Failed to load config [$fileName] from assets", throwable)
                emptyMap()
            },
        )
    }

    fun sendException(fileName: String, isParsingSuccess: Boolean, json: String?) {
        analyticsExceptionHandler.sendException(
            ExceptionAnalyticsEvent(
                exception = IllegalStateException("Parsing config is failed"),
                params = mapOf(
                    "filename" to fileName,
                    "isParsingSuccess" to isParsingSuccess.toString(),
                    "json_size" to json?.length.toString(),
                    "json" to json?.take(n = 30).toString(),
                ),
            ),
        )
    }
}