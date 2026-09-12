@file:OptIn(ExperimentalStdlibApi::class)

package com.tangem.datasource.local.assets

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.core.local.assets.AssetLoader
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
import java.lang.reflect.Type
import javax.inject.Inject
import kotlin.reflect.KType

internal class DefaultAssetLoader @Inject constructor(
    private val assetReader: AssetReader,
    @NetworkMoshi private val moshi: Moshi,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
    private val dispatchers: CoroutineDispatcherProvider,
) : AssetLoader {

    @Suppress("SuspendFunSwallowedCancellation")
    override suspend fun <T> load(fileName: String, type: KType): T? = withContext(dispatchers.io) {
        val json = runCatching { assetReader.read(fullFileName = "$fileName.json") }.getOrNull()

        runCatching {
            moshi.adapter<T>(type).fromJson(json)
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