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
            ApiEnvironment.DEV_NEW -> 1
            ApiEnvironment.DEV_2 -> 2
            ApiEnvironment.DEV_2_NEW -> 3
            ApiEnvironment.DEV_3 -> 4
            ApiEnvironment.DEV_3_NEW -> 5
            ApiEnvironment.STAGE -> 6
            ApiEnvironment.DEV_4_NEW -> 7
            ApiEnvironment.STAGE_2 -> 8
            ApiEnvironment.DEV_5_NEW -> 9
            ApiEnvironment.STAGE_3 -> 10
            ApiEnvironment.DEV_6_NEW -> 11
            ApiEnvironment.MOCK -> 12
            ApiEnvironment.PROD -> 13
            ApiEnvironment.PROD_NEW -> 14
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