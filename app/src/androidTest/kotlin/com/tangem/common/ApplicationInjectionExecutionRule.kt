package com.tangem.common

import androidx.test.core.app.ApplicationProvider
import com.tangem.tap.ApplicationEntryPoint
import com.tangem.tap.TangemApplication
import dagger.hilt.android.testing.OnComponentReadyRunner
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import timber.log.Timber

class ApplicationInjectionExecutionRule(
    private val toggleStates: Map<String, Boolean>
) : TestRule {

    private val tangemApplication: TangemApplication
        get() = ApplicationProvider.getApplicationContext()

    private var originalFeatureTogglesValues: Map<String, String>? = null

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                saveOriginalFeatureToggles()
                overrideFeatureToggles()

                OnComponentReadyRunner.addListener(
                    tangemApplication, ApplicationEntryPoint::class.java
                ) { _: ApplicationEntryPoint ->
                    tangemApplication.preInit()
                    tangemApplication.init()
                }

                try {
                    base.evaluate()
                } finally {
                    restoreOriginalFeatureToggles()
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun saveOriginalFeatureToggles() {
        try {
            val featureTogglesClass = Class.forName("com.tangem.core.configtoggle.FeatureToggles")
            val valuesField = featureTogglesClass.getDeclaredField("values")
            valuesField.isAccessible = true
            originalFeatureTogglesValues = valuesField.get(null) as Map<String, String>
        } catch (e: Exception) {
            Timber.e("Failed to save original toggles values: ${e.message}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun overrideFeatureToggles() {
        try {
            val featureTogglesClass = Class.forName("com.tangem.core.configtoggle.FeatureToggles")
            val valuesField = featureTogglesClass.getDeclaredField("values")
            valuesField.isAccessible = true

            val originalValues = originalFeatureTogglesValues ?:
            (valuesField.get(null) as Map<String, String>)

            val newValues = originalValues.toMutableMap()

            toggleStates.forEach { (toggle, enabled) ->
                if (enabled) {
                    newValues[toggle] = "1.0.0"
                } else {
                    newValues.remove(toggle)
                }
            }

            valuesField.set(null, newValues)

            Timber.i("FeatureToggles.values updated: $toggleStates")

        } catch (e: Exception) {
            Timber.e("FeatureToggles.values didn't change with error: ${e.message}")
        }
    }

    private fun restoreOriginalFeatureToggles() {
        try {
            if (originalFeatureTogglesValues != null) {
                val featureTogglesClass = Class.forName("com.tangem.core.configtoggle.FeatureToggles")
                val valuesField = featureTogglesClass.getDeclaredField("values")
                valuesField.isAccessible = true
                valuesField.set(null, originalFeatureTogglesValues)
                Timber.i("FeatureToggles.values restored")
            }
        } catch (e: Exception) {
            Timber.e("FeatureToggles.values didn't restored with error: ${e.message}")
        }
    }
}