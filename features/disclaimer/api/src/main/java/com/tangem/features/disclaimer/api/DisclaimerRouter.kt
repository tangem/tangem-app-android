package com.tangem.features.disclaimer.api

import androidx.fragment.app.Fragment

interface DisclaimerRouter {

    fun entryFragment(): Fragment

    companion object {
        const val IS_TOS_ACCEPTED_KEY = "is_tos_accepted"
    }
}