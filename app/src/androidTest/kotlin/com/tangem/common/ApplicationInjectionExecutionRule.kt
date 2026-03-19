package com.tangem.common

import androidx.test.core.app.ApplicationProvider
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.tap.ApplicationEntryPoint
import com.tangem.tap.TangemApplication
import dagger.hilt.android.testing.OnComponentReadyRunner
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import timber.log.Timber
import java.lang.reflect.Field

class ApplicationInjectionExecutionRule(
    private val toggleStates: Map<String, Boolean>,
) : TestRule {

    private val tangemApplication: TangemApplication
        get() = ApplicationProvider.getApplicationContext()

    private var originalVersionValues: Map<FeatureToggles, String>? = null

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                saveOriginalFeatureToggles()
                overrideFeatureToggles()

                OnComponentReadyRunner.addListener(
                    tangemApplication, ApplicationEntryPoint::class.java,
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

    private fun saveOriginalFeatureToggles() {
        try {
            originalVersionValues = FeatureToggles.entries.associateWith { it.version }
        } catch (e: Exception) {
            Timber.e("Failed to save original toggles values: ${e.message}")
        }
    }

    private fun overrideFeatureToggles() {
        try {
            val versionField = getVersionField()

            FeatureToggles.entries.forEach { toggle ->
                val enabled = toggleStates[toggle.rawName] ?: return@forEach
                val newVersion = if (enabled) ENABLED_VERSION else DISABLED_VERSION
                versionField.set(toggle, newVersion)
            }

            Timber.i("FeatureToggles.values updated: $toggleStates")
        } catch (e: Exception) {
            Timber.e("FeatureToggles.values didn't change with error: ${e.message}")
        }
    }

    private fun restoreOriginalFeatureToggles() {
        try {
            val saved = originalVersionValues ?: return
            val versionField = getVersionField()

            saved.forEach { (toggle, originalVersion) ->
                versionField.set(toggle, originalVersion)
            }

            Timber.i("FeatureToggles.values restored")
        } catch (e: Exception) {
            Timber.e("FeatureToggles.values didn't restored with error: ${e.message}")
        }
    }

    private fun getVersionField(): Field {
        val field = FeatureToggles::class.java.getDeclaredField("version")
        field.isAccessible = true
        return field
    }

    private companion object {
        const val ENABLED_VERSION = "1.0.0"
        const val DISABLED_VERSION = "undefined"
    }
}