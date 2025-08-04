package com.tangem.common.rules

import androidx.test.platform.app.InstrumentationRegistry
import com.tangem.common.annotations.ApiEnv
import com.tangem.datasource.api.common.config.ApiConfig
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
 * This rule allows tests to specify which API environment to use either through annotations on the test method/class
 * or via an instrumentation argument.
 *
 * This rule supports @ApiEnv annotation with a map of API configs to configure different environments.
 * For any ApiConfig.ID not specified in annotations, MOCK environment will be used by default.
 */
class ApiEnvironmentRule : TestRule {

    private var targetEnvironments: Map<ApiConfig.ID, ApiEnvironment> = emptyMap()

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

        mutableManager.setupEnvironments()
    }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                targetEnvironments = determineEnvironments(description)
                base.evaluate()
            }
        }
    }

    private fun determineEnvironments(description: Description): Map<ApiConfig.ID, ApiEnvironment> {
        val instrumentationArgs = InstrumentationRegistry.getArguments()
        val envConfigArg = instrumentationArgs.getString(ENV_CONFIGS_ARGUMENT)

        if (!envConfigArg.isNullOrEmpty()) {
            return parseEnvironmentConfigs(envConfigArg)
        }

        val methodEnvironments = collectApiEnvAnnotations(description)
        if (methodEnvironments.isNotEmpty()) {
            return DEFAULT_API_CONFIGS.associateWith { ApiEnvironment.MOCK } + methodEnvironments
        }

        val classEnvironments = collectApiEnvAnnotations(description.testClass)
        if (classEnvironments.isNotEmpty()) {
            return DEFAULT_API_CONFIGS.associateWith { ApiEnvironment.MOCK } + classEnvironments
        }

        return DEFAULT_API_CONFIGS.associateWith { ApiEnvironment.MOCK }
    }

    private fun parseEnvironmentConfigs(envConfigArg: String): Map<ApiConfig.ID, ApiEnvironment> {
        return try {
            val parsedConfigs = envConfigArg.split(",")
                .map { it.trim() }
                .mapNotNull { configPair ->
                    val parts = configPair.split("=").map { it.trim() }
                    if (parts.size == 2) {
                        try {
                            val apiConfigId = ApiConfig.ID.valueOf(parts[0])
                            val environment = ApiEnvironment.valueOf(parts[1])
                            apiConfigId to environment
                        } catch (e: IllegalArgumentException) {
                            Timber.w("Invalid config or environment: $configPair")
                            null
                        }
                    } else {
                        Timber.w("Invalid config format: $configPair. Expected format: 'ConfigId=Environment'")
                        null
                    }
                }
                .toMap()

            DEFAULT_API_CONFIGS.associateWith { ApiEnvironment.MOCK } + parsedConfigs
        } catch (e: Exception) {
            Timber.w("Failed to parse environment configs: $envConfigArg")
            DEFAULT_API_CONFIGS.associateWith { ApiEnvironment.MOCK }
        }
    }

    private fun collectApiEnvAnnotations(description: Description): Map<ApiConfig.ID, ApiEnvironment> {
        return description.getAnnotation(ApiEnv::class.java)?.value
            ?.associate { it.apiConfigId to it.environment } ?: emptyMap()
    }

    private fun collectApiEnvAnnotations(testClass: Class<*>): Map<ApiConfig.ID, ApiEnvironment> {
        return testClass.getAnnotation(ApiEnv::class.java)?.value
            ?.associate { it.apiConfigId to it.environment } ?: emptyMap()
    }

    private fun MutableApiConfigsManager.setupEnvironments() {
        require(targetEnvironments.isNotEmpty()) { "Target environments map is empty" }

        runBlocking {
            targetEnvironments.forEach { (apiConfigId, environment) ->
                changeEnvironment(apiConfigId.name, environment)
                Timber.i("$apiConfigId environment set to: ${environment.name}")
            }
        }
    }

    private companion object {
        const val ENV_CONFIGS_ARGUMENT = "testEnvironmentConfigs"

        val DEFAULT_API_CONFIGS = listOf(
            ApiConfig.ID.TangemTech,
            ApiConfig.ID.Express,
            ApiConfig.ID.TangemPay,
        )
    }
}