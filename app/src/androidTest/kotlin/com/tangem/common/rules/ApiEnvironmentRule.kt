package com.tangem.common.rules

import androidx.test.platform.app.InstrumentationRegistry
import com.tangem.common.annotations.ApiEnv
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.api.common.config.managers.MutableApiConfigsManager
import com.tangem.wallet.test.BuildConfig
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import timber.log.Timber

/**
 * A JUnit rule that sets up the API environment for tests based on annotations or instrumentation arguments.
 *
 * This rule allows tests to specify which API environment to use either through an annotation on the test method/class
 * or via an instrumentation argument.
 *
 * This rule only finds and stores the required API environment. The actual environment setup is performed
 * by calling the [setup] method with an appropriate [ApiConfigsManager] instance.
 */
class ApiEnvironmentRule : TestRule {

    private var targetEnvironment: ApiEnvironment? = null

    /**
     * Sets up the API environment based on the provided [ApiConfigsManager].
     * This method is typically called in the test setup phase to ensure the correct API environment is configured.
     *
     * @param apiConfigsManager the manager responsible for API configurations
     */
    fun setup(apiConfigsManager: ApiConfigsManager) {
        val mutableManager = requireNotNull(apiConfigsManager as? MutableApiConfigsManager) {
            "MutableApiConfigsManager isn't available in build type ${BuildConfig.BUILD_TYPE}."
        }

        mutableManager.setupEnvironment()
    }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                targetEnvironment = determineEnvironment(description)

                base.evaluate()
            }
        }
    }

    private fun determineEnvironment(description: Description): ApiEnvironment {
        val instrumentationArgs = InstrumentationRegistry.getArguments()
        val envArg = instrumentationArgs.getString(ENV_ARGUMENT)

        if (!envArg.isNullOrEmpty()) return ApiEnvironment.valueOf(envArg.uppercase())

        val methodAnnotation = description.getAnnotation(ApiEnv::class.java)
        if (methodAnnotation != null) return methodAnnotation.environment

        val classAnnotation = description.testClass.getAnnotation(ApiEnv::class.java)
        if (classAnnotation != null) return classAnnotation.environment

        return ApiEnvironment.MOCK
    }

    private fun MutableApiConfigsManager.setupEnvironment() {
        val environment = requireNotNull(targetEnvironment) { "Target environment is null" }

        runBlocking { changeEnvironment(environment) }

        Timber.i("API environment set to: ${environment.name}")
    }

    private companion object {
        const val ENV_ARGUMENT = "testEnvironment"
    }
}