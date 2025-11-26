package com.tangem.core.abtests.manager.impl

import android.app.Application
import com.amplitude.experiment.Experiment
import com.amplitude.experiment.ExperimentClient
import com.amplitude.experiment.ExperimentConfig
import com.amplitude.experiment.ExperimentUser
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.utils.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

internal class AmplitudeABTestsManager(
    val application: Application,
    val apiKeyProvider: Provider<String>,
    val scope: CoroutineScope,
) : ABTestsManager {

    private lateinit var client: ExperimentClient

    override fun init() {
        if (::client.isInitialized) {
            Timber.w("AB Tests manager already initialized, skipping")
            return
        }

        client = Experiment.initializeWithAmplitudeAnalytics(
            application = application,
            apiKey = apiKeyProvider(),
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
                Timber.e(exception, "Failed to fetch AB test variants")
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
        Timber.d("=".repeat(SEPARATOR_LENGTH))
        Timber.d("AB Tests: Fetched ${allVariants.size} variants")
        Timber.d("=".repeat(SEPARATOR_LENGTH))

        if (allVariants.isEmpty()) {
            Timber.d("No variants available")
        } else {
            allVariants.entries.forEachIndexed { index, (key, variant) ->
                Timber.d("[${index + 1}/${allVariants.size}] Key: $key")
                Timber.d("  → Value: ${variant.value ?: "null"}")
                Timber.d("  → Payload: ${variant.payload ?: "null"}")
                Timber.d("  → Key: ${variant.key ?: "null"}")
                Timber.d("-".repeat(SEPARATOR_LENGTH))
            }
        }

        Timber.d("=".repeat(SEPARATOR_LENGTH))
    }

    private companion object {
        const val SEPARATOR_LENGTH = 50
    }
}