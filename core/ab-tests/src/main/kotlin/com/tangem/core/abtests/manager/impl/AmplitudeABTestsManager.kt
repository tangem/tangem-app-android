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
import kotlinx.coroutines.launch

internal class AmplitudeABTestsManager(
    val application: Application,
    val apiKey: String,
    val scope: AppCoroutineScope,
) : ABTestsManager {

    private lateinit var client: ExperimentClient

    override fun init() {
        if (::client.isInitialized) {
            TangemLogger.w("AB Tests manager already initialized, skipping")
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
                TangemLogger.e("Failed to fetch AB test variants", exception)
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

    override fun getValue(key: String, defaultValue: String): String {
        return client.variant(key).value ?: defaultValue
    }

    private fun logAllVariants(allVariants: Map<String, com.amplitude.experiment.Variant>) {
        TangemLogger.d("=".repeat(SEPARATOR_LENGTH))
        TangemLogger.d("AB Tests: Fetched ${allVariants.size} variants")
        TangemLogger.d("=".repeat(SEPARATOR_LENGTH))

        if (allVariants.isEmpty()) {
            TangemLogger.d("No variants available")
        } else {
            allVariants.entries.forEachIndexed { index, (key, variant) ->
                TangemLogger.d("[${index + 1}/${allVariants.size}] Key: $key")
                TangemLogger.d("  → Value: ${variant.value ?: "null"}")
                TangemLogger.d("  → Payload: ${variant.payload ?: "null"}")
                TangemLogger.d("  → Key: ${variant.key ?: "null"}")
                TangemLogger.d("-".repeat(SEPARATOR_LENGTH))
            }
        }

        TangemLogger.d("=".repeat(SEPARATOR_LENGTH))
    }

    private companion object {
        const val SEPARATOR_LENGTH = 50
    }
}