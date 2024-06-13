package com.tangem.features.tester.api

import android.content.Intent

/**
 * Outer tester feature router
 *
 * @author Andrew Khokhlov on 07/02/2023
 */
interface TesterRouter {

    /** Open tester menu */
    fun getEntryIntent(): Intent
}
