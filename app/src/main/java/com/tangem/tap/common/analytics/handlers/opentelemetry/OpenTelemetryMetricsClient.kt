package com.tangem.tap.common.analytics.handlers.opentelemetry

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.wallet.BuildConfig
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.contrib.disk.buffering.exporters.MetricToDiskExporter
import io.opentelemetry.contrib.disk.buffering.exporters.callback.ExporterCallback
import io.opentelemetry.contrib.disk.buffering.storage.impl.FileMetricStorage
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * OpenTelemetry metrics pipeline for mirroring business metrics to the Tangem OTLP gateway.
 *
 * Metrics are recorded with delta temporality and flushed to disk every [EXPORT_INTERVAL_SECONDS]
 * (so batches survive offline periods and process death), then drained to the OTLP endpoint right
 * after each flush. A batch that fails to upload is written back and retried on the next drain.
 * The resource carries only the app coordinates — no user, device or session identifiers, per the
 * app privacy policy.
 */
class OpenTelemetryMetricsClient private constructor(
    application: Application,
    apiKey: String,
    private val scope: CoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    private val storage = FileMetricStorage.create(File(application.cacheDir, STORAGE_DIR_NAME))

    private val otlpExporter = OtlpHttpMetricExporter.builder()
        .setEndpoint(ENDPOINT)
        .addHeader(API_KEY_HEADER, apiKey)
        .setAggregationTemporalitySelector(AggregationTemporalitySelector.deltaPreferred())
        .build()

    private val meterProvider: SdkMeterProvider = SdkMeterProvider.builder()
        .setResource(
            Resource.create(
                Attributes.builder()
                    .put(SERVICE_NAME_KEY, SERVICE_NAME)
                    .put(SERVICE_VERSION_KEY, BuildConfig.VERSION_NAME)
                    .put(OS_NAME_KEY, OS_NAME)
                    .build(),
            ),
        )
        .registerMetricReader(
            PeriodicMetricReader.builder(
                MetricToDiskExporter.builder(storage)
                    .setAggregationTemporalitySelector(AggregationTemporalitySelector.deltaPreferred())
                    .setExporterCallback(DrainOnFlushCallback())
                    .build(),
            )
                .setInterval(Duration.ofSeconds(EXPORT_INTERVAL_SECONDS))
                .build(),
        )
        .build()

    private val drainMutex = Mutex()

    fun getMeter(): Meter = meterProvider.get(INSTRUMENTATION_SCOPE_NAME)

    fun flush() {
        meterProvider.forceFlush()
    }

    private fun start(application: Application) {
        scope.launch(dispatchers.io) { drainStorage() }
        application.registerComponentCallbacks(FlushOnBackgroundCallbacks())
    }

    private suspend fun drainStorage() {
        drainMutex.withLock {
            // batches are deleted from disk as they are iterated (deleteItemsOnIteration default)
            for (batch in storage) {
                if (batch.isEmpty()) continue
                val result = otlpExporter.export(batch).join(EXPORT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                if (!result.isSuccess) {
                    storage.write(batch)
                    break
                }
            }
        }
    }

    private inner class DrainOnFlushCallback : ExporterCallback<MetricData> {
        override fun onExportSuccess(items: Collection<MetricData>) {
            scope.launch(dispatchers.io) { drainStorage() }
        }

        override fun onExportError(items: Collection<MetricData>, error: Throwable?) = Unit

        override fun onShutdown() = Unit
    }

    private inner class FlushOnBackgroundCallbacks : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) {
            if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                scope.launch(dispatchers.io) { flush() }
            }
        }

        override fun onConfigurationChanged(newConfig: Configuration) = Unit

        @Deprecated("Deprecated in Java")
        override fun onLowMemory() = Unit
    }

    companion object {

        @Volatile
        var instance: OpenTelemetryMetricsClient? = null
            private set

        private const val ENDPOINT = "https://otlp.services.tangem.org/v1/metrics"
        private const val API_KEY_HEADER = "x-api-key"
        private const val EXPORT_INTERVAL_SECONDS = 300L
        private const val EXPORT_TIMEOUT_SECONDS = 30L
        private const val STORAGE_DIR_NAME = "otel-metrics"
        private const val INSTRUMENTATION_SCOPE_NAME = "com.tangem.analytics"
        private const val SERVICE_NAME = "tangem-app-android"
        private const val OS_NAME = "android"

        private val SERVICE_NAME_KEY = AttributeKey.stringKey("service.name")
        private val SERVICE_VERSION_KEY = AttributeKey.stringKey("service.version")
        private val OS_NAME_KEY = AttributeKey.stringKey("os.name")

        fun initialize(
            application: Application,
            apiKey: String,
            scope: CoroutineScope,
            dispatchers: CoroutineDispatcherProvider,
        ) {
            // the storage touches the disk on creation, so the whole setup runs off the main thread
            scope.launch(dispatchers.io) {
                instance = OpenTelemetryMetricsClient(application, apiKey, scope, dispatchers)
                    .apply { start(application) }
            }
        }
    }
}