package com.tangem.core.abtests.manager.impl

import android.app.Application
import com.amplitude.experiment.Experiment
import com.amplitude.experiment.ExperimentClient
import com.amplitude.experiment.ExperimentConfig
import com.amplitude.experiment.ExperimentUser
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal class AmplitudeABTestsManager(
    val application: Application,
    val apiKey: String,
    val scope: AppCoroutineScope,
) : ABTestsManager {

    private lateinit var client: ExperimentClient

    private val variantsFetched = CompletableDeferred<Unit>()

    private val logger = TangemLogger.withTag(TAG)

    override fun init() {
        if (::client.isInitialized) {
            logger.w("AB Tests manager already initialized, skipping")
            return
        }

        client = Experiment.initializeWithAmplitudeAnalytics(
            application = application,
            apiKey = apiKey,
            config = ExperimentConfig
                .builder()
                .automaticFetchOnAmplitudeIdentityChange(true)
                .build(),
        )

        scope.launch {
            try {
                client.fetch().get()
                val allVariants = client.all()
                logAllVariants(allVariants)
            } catch (exception: Exception) {
                logger.e("Failed to fetch AB test variants", exception)
            } finally {
                variantsFetched.complete(Unit)
            }
        }
    }

    override fun setUserProperties(userId: String?, batch: String?, productType: String?, firmware: String?) {
        val userProperties = mutableMapOf<String, Any>()
        batch?.let { userProperties[AnalyticsParam.BATCH] = it }
        productType?.let { userProperties[AnalyticsParam.PRODUCT_TYPE] = it }
        firmware?.let { userProperties[AnalyticsParam.FIRMWARE] = it }

        client.setUser(
            ExperimentUser
                .builder()
                .userId(userId)
                .userProperties(userProperties)
                .build(),
        )
    }

    override fun removeUserProperties() {
        client.setUser(ExperimentUser())
    }

    override suspend fun getValue(key: String, defaultValue: String): String {
        if (!::client.isInitialized) return defaultValue
        awaitVariantsFetched()
        return client.variant(key).value ?: defaultValue
    }

    private suspend fun awaitVariantsFetched() {
        if (variantsFetched.isCompleted) return
        val completed = withTimeoutOrNull(FETCH_AWAIT_TIMEOUT_MILLIS) {
            variantsFetched.await()
        }
        if (completed == null) {
            logger.w("AB Tests variants not fetched within $FETCH_AWAIT_TIMEOUT_MILLIS ms, using default value")
            // Prevent repeated blocking on subsequent calls; fetch can still complete in background.
            variantsFetched.complete(Unit)
        }
    }

    private fun logAllVariants(allVariants: Map<String, com.amplitude.experiment.Variant>) {
        val message = buildString {
            appendLine("AB Tests: Fetched ${allVariants.size} variants")
            if (allVariants.isEmpty()) {
                append("No variants available")
            } else {
                allVariants.entries.forEachIndexed { index, (key, variant) ->
                    appendLine("[${index + 1}/${allVariants.size}] $key")
                    appendLine("  → value: ${variant.value ?: "null"}")
                    appendLine("  → key: ${variant.key ?: "null"}")
                    append("  → payload: ${variant.payload ?: "null"}")
                    if (index != allVariants.size - 1) appendLine()
                }
            }
        }

        logger.i(message)
    }

    private companion object {
        const val TAG = "AmplitudeABTestsManager"
        const val FETCH_AWAIT_TIMEOUT_MILLIS = 3_000L
    }
}