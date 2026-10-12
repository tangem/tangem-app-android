package com.tangem.core.abtests

/**
 * Configuration required by the A/B tests subsystem. Supplied by the application layer so that
 * `core:ab-tests` does not depend on the environment-config source.
 *
 * @property amplitudeApiKey production Amplitude API key.
 * @property amplitudeApiKeyDev Amplitude API key used in tester builds.
 */
data class ABTestsConfig(
    val amplitudeApiKey: String,
    val amplitudeApiKeyDev: String?,
)