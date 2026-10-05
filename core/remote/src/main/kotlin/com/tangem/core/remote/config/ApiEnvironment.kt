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
    STAGE,
    STAGE_2,
    STAGE_3,
    MOCK,
    PROD,

    /** Replacement for [DEV] */
    DEV_NEW,

    /** Replacement for [DEV_2] */
    DEV_2_NEW,

    /** Replacement for [DEV_3] */
    DEV_3_NEW,

    /** Replacement for [STAGE] (backend name: stage1) */
    DEV_4_NEW,

    /** Replacement for [STAGE_2] (backend name: stage2) */
    DEV_5_NEW,

    /** Replacement for [STAGE_3] (backend name: stage3) */
    DEV_6_NEW,

    /** Temporary host for checking [PROD] before the switch */
    PROD_NEW,
}