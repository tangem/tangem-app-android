@file:OptIn(ExperimentalStdlibApi::class)

package com.tangem.datasource.local.assets

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.core.local.assets.AssetLoader
import com.tangem.core.remote.json.NetworkJson
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import java.lang.reflect.Type
import javax.inject.Inject
import kotlin.reflect.KType

internal class DefaultAssetLoader @Inject constructor(
    private val assetReader: AssetReader,
    @NetworkMoshi private val moshi: Moshi,
    @NetworkJson private val json: Json,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
    private val dispatchers: CoroutineDispatcherProvider,
) : AssetLoader {

    override suspend fun <T> load(fileName: String, type: KType): T? {
        return loadObject(fileName) { content -> moshi.adapter<T>(type).fromJson(content) }
    }

    override suspend fun <T> load(fileName: String, deserializer: DeserializationStrategy<T>): T? {
        return loadObject(fileName) { content -> json.decodeFromString(deserializer, content) }
    }

    @Suppress("SuspendFunSwallowedCancellation")
    private suspend fun <T> loadObject(fileName: String, parse: (String) -> T?): T? = withContext(dispatchers.io) {
        val content = runCatching { assetReader.read(fullFileName = "$fileName.json") }.getOrNull()

        runCatching {
            parse(requireNotNull(content) { "Asset [$fileName] is not read" })
        }
            .fold(
                onSuccess = { parsedConfig ->
                    if (parsedConfig == null) {
                        sendException(
                            fileName = fileName,
                            isParsingSuccess = true,
                            json = content,
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
                        json = content,
                    )

                    TangemLogger.e("Failed to load config [$fileName] from assets", throwable)
                    null
                },
            )
    }

    override suspend fun <T> loadList(fileName: String, type: Type): List<T> {
        val result = runCatching(dispatchers.io) {
            val json = assetReader.read(fullFileName = "$fileName.json")

            moshi.adapter<List<T>>(type).fromJson(json)
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

    override suspend fun <T> loadMap(fileName: String, type: Type): Map<String, T> {
        val result = runCatching(dispatchers.io) {
            val json = assetReader.read(fullFileName = "$fileName.json")

            moshi.adapter<Map<String, T>>(type).fromJson(json)
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

    private fun sendException(fileName: String, isParsingSuccess: Boolean, json: String?) {
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