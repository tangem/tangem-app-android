package com.tangem.features.tokendetails.navigation

import androidx.fragment.app.Fragment

interface TokenDetailsRouter {

    fun getEntryFragment(): Fragment

    companion object {
        const val TOKEN_DETAILS_ARGS = "token_details_args"
    }
}