package com.tangem.common.module

/**
 * Created by Anton Zhilenkov on 18/04/2022.
 * A module exception
 */
interface ModuleException {
    val message: String
}

/**
 * An exception marked as FbConsumeException should be submitted to Firebase.Crashlytics as a non-fatal issue.
 */
interface FbConsumeException
