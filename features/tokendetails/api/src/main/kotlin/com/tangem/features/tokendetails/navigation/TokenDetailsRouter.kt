package com.tangem.features.tokendetails.navigation

import androidx.fragment.app.Fragment

interface TokenDetailsRouter {

    fun getEntryFragment(): Fragment

    companion object {
        const val SELECTED_CURRENCY_KEY = "selected_currency"
    }
}
