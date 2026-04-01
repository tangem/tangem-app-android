package com.tangem.plugin.configuration.utils

import com.tangem.plugin.configuration.model.BuildConfigField
import org.gradle.api.Project

private const val WIREMOCK_LOCAL_URL_PROPERTY = "wiremockLocalUrl"

/**
 * Resolves gradle properties for build config fields that support overrides.
 *
 * Supported overrides:
 * - [BuildConfigField.WireMockLocalUrl] — override via `-PwiremockLocalUrl=http://localhost:8080`
 */
internal fun List<BuildConfigField>.resolveGradleProperties(project: Project): List<BuildConfigField> {
    return map { field ->
        when (field) {
            is BuildConfigField.WireMockLocalUrl -> {
                val url = project.findProperty(WIREMOCK_LOCAL_URL_PROPERTY) as? String ?: return@map field
                BuildConfigField.WireMockLocalUrl(url = url)
            }
            else -> field
        }
    }
}