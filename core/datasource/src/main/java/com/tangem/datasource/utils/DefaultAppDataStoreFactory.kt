package com.tangem.datasource.utils

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.core.local.datastore.KotlinxDataStoreSerializer
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import java.io.File
import javax.inject.Inject

/**
 * Default [AppDataStoreFactory].
 *
 * When the persisted file cannot be parsed, the [Serializer.readFrom] throws a [CorruptionException]; the store then
 * replaces the corrupted file with [Serializer.defaultValue] and reports the failure to [analyticsExceptionHandler]
 * as a non-fatal event enriched with diagnostics (file name, size, corruption kind and a short head sample), so its
 * frequency and the affected file stay observable.
 */
internal class DefaultAppDataStoreFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
    private val appScope: AppCoroutineScope,
) : AppDataStoreFactory {

    override fun <T> create(defaultValue: T, serializer: KSerializer<T>, fileName: String): DataStore<T> {
        return create(
            serializer = KotlinxDataStoreSerializer(defaultValue = defaultValue, serializer = serializer),
            scope = appScope,
            produceFile = { context.dataStoreFile(fileName) },
        )
    }

    override fun <T> create(serializer: Serializer<T>, scope: CoroutineScope, produceFile: () -> File): DataStore<T> {
        val file = produceFile()
        return DataStoreFactory.create(
            serializer = serializer,
            corruptionHandler = ReplaceFileCorruptionHandler { exception ->
                analyticsExceptionHandler.sendException(corruptionEvent(file = file, cause = exception))
                serializer.defaultValue
            },
            scope = scope,
            produceFile = { file },
        )
    }

    private fun corruptionEvent(file: File, cause: Throwable): ExceptionAnalyticsEvent {
        val bytes = runCatching { file.readBytes() }.getOrNull()
        return ExceptionAnalyticsEvent(
            exception = DataStoreCorruptionException(fileName = file.name, cause = cause).withGroupingFrame(file.name),
            params = mapOf(
                "file" to file.name,
                "size" to (bytes?.size ?: -1).toString(),
                "kind" to bytes.classifyCorruption(),
                "head" to bytes?.take(HEAD_SAMPLE_BYTES)?.toByteArray()?.decodeToString().orEmpty(),
                "tmp_exists" to File(file.path + ".tmp").exists().toString(),
            ),
        )
    }

    /**
     * Prepends a synthetic stack frame named after [frame] so that corruption of different files is grouped into
     * distinct issues by the crash reporter instead of collapsing into a single one at the throw site.
     */
    private fun <T : Throwable> T.withGroupingFrame(frame: String): T = apply {
        stackTrace = arrayOf(
            StackTraceElement(DataStoreCorruptionException::class.java.name, frame, frame, 0),
        ) + stackTrace
    }

    private fun ByteArray?.classifyCorruption(): String = when {
        this == null -> "unreadable"
        isEmpty() -> "empty"
        all { it == ZERO_BYTE } -> "zeroed"
        first().toInt().toChar().let { it != '{' && it != '[' } -> "non_json"
        else -> "malformed_json"
    }

    private companion object {
        const val HEAD_SAMPLE_BYTES = 32
        const val ZERO_BYTE: Byte = 0
    }
}

/** Non-fatal marker reported when a DataStore file is corrupted and replaced with its default value. */
internal class DataStoreCorruptionException(fileName: String, cause: Throwable) :
    Exception("Corrupted DataStore file replaced with default value: $fileName", cause)