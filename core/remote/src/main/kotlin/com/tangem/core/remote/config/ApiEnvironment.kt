package com.tangem.core.remote.config

import kotlinx.serialization.Serializable

/**
 * Api environment.
 *
 * Serialized by name (e.g. "DEV") when persisting the selected environment; the constant names are
 * the wire values.
 *
[REDACTED_AUTHOR]
 */
@Serializable
enum class ApiEnvironment {
    DEV,
    DEV_2,
    DEV_3,
    DEV_4,
    DEV_5,
    DEV_6,
    STAGE,
    MOCK,
    PROD,
}