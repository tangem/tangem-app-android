package com.tangem.features.tester.api

import android.content.Intent

/**
 * Outer tester feature router
 *
[REDACTED_AUTHOR]
 */
interface TesterRouter {

    /** Open tester menu */
    fun getEntryIntent(): Intent
}