package com.tangem.feature.tester.presentation.environments.utils

import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.ApiEnvironmentConfig

/**
 * Comparator for [ApiEnvironmentConfig] based on the priority of [ApiEnvironment].
 *
[REDACTED_AUTHOR]
 */
internal object ApiEnvironmentComparator : Comparator<ApiEnvironmentConfig> {

    private val apiEnvironmentPriorityMap = ApiEnvironment.entries.associateWith {
        when (it) {
            ApiEnvironment.DEV -> 0
            ApiEnvironment.DEV_2 -> 1
            ApiEnvironment.STAGE -> 2
            ApiEnvironment.MOCK -> 3
            ApiEnvironment.PROD -> 4
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