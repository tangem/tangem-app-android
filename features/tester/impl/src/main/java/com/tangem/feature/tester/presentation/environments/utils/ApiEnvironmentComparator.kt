package com.tangem.feature.tester.presentation.environments.utils

import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig

/**
 * Comparator for [ApiEnvironmentConfig] based on the priority of [ApiEnvironment].
 *
[REDACTED_AUTHOR]
 */
internal object ApiEnvironmentComparator : Comparator<ApiEnvironmentConfig> {

    private val apiEnvironmentPriorityMap = ApiEnvironment.entries.associateWith { environment ->
        when (environment) {
            ApiEnvironment.DEV -> 0
            ApiEnvironment.DEV_2 -> 1
            ApiEnvironment.DEV_3 -> 2
            ApiEnvironment.DEV_4 -> 3
            ApiEnvironment.DEV_5 -> 4
            ApiEnvironment.DEV_6 -> 5
            ApiEnvironment.STAGE -> 6
            ApiEnvironment.MOCK -> 7
            ApiEnvironment.PROD -> 8
        }
    }

    override fun compare(o1: ApiEnvironmentConfig, o2: ApiEnvironmentConfig): Int {
        val priority1: Int = getPriority(environment = o1.environment)
        val priority2: Int = getPriority(environment = o2.environment)

        return priority1.compareTo(priority2)
    }

    private fun getPriority(environment: ApiEnvironment): Int {
        return apiEnvironmentPriorityMap[environment] ?: error("Unknown environment: $environment")
    }
}