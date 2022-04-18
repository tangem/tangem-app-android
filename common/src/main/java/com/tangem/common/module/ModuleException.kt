package com.tangem.common.module

/**
[REDACTED_AUTHOR]
 * A module exception
 */
interface ModuleException {
    val message: String
}

/**
 * An exception marked as FbConsumeException should be submitted to Firebase.Crashlytics as a non-fatal issue.
 */
interface FbConsumeException